package org.spf4j.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.apache.calcite.schema.SchemaPlus;
import org.glassfish.hk2.api.Immediate;
import org.jvnet.hk2.annotations.Service;
import org.spf4j.actuator.cluster.metrics.MetricsClusterResource;
import org.spf4j.avro.AvroDataSet;
import org.spf4j.avro.SqlPredicate;
import org.spf4j.avro.calcite.AvroDataSetAsProjectableFilterableTable;
import org.spf4j.base.CloseableIterable;
import org.spf4j.jaxrs.aql.AvroQueryResource;
import org.spf4j.perf.MeasurementStore;
import org.spf4j.perf.MeasurementStoreQuery;
import org.spf4j.perf.MeasurementsInfo;
import org.spf4j.tsdb2.TableDefs;
import org.spf4j.security.AbacSecurityContext;

/**
 * @author Zoltan Farkas
 */
@Immediate
@Service
public class MetricsQueryRegister {

  private static volatile Store store;

  private final AvroQueryResource queryResource;

  private final MetricsClusterResource clusterResource;

  @Inject
  public MetricsQueryRegister(final AvroQueryResource queryResource,
          final MetricsClusterResource clusterResource) {
    this.queryResource = queryResource;
    this.clusterResource = clusterResource;
    MeasurementCollector x = store.replace(this::registerMetricDataSet);
    for (MeasurementRegistrationInfo info : x.getCollection()) {
      registerMetricDataSet(info);
    }
  }

  private void registerMetricDataSet(final MeasurementRegistrationInfo info) {
    SchemaPlus defaultSchema = queryResource.getConfig().getDefaultSchema();
    String tableName = info.mInfo.getMeasuredEntity().toString();
    defaultSchema.add(tableName,
            new AvroDataSetAsProjectableFilterableTable(
            new MetricDataSet(info, clusterResource)));
  }

  public static class MeasurementRegistrationInfo {
    MeasurementsInfo mInfo;
    int sampleTimeMillis;
    long registrationId;

    public MeasurementRegistrationInfo(MeasurementsInfo mInfo, int sampleTimeMillis, long registrationId) {
      this.mInfo = mInfo;
      this.sampleTimeMillis = sampleTimeMillis;
      this.registrationId = registrationId;
    }


  }


  public interface MeasurementCollector extends Consumer<MeasurementRegistrationInfo> {
    default Collection<MeasurementRegistrationInfo> getCollection() {
      throw new UnsupportedOperationException();
    }
  }

  public static class Store implements MeasurementStore {

    private final AtomicReference<MeasurementCollector> metrics;

    private final MeasurementStore wrapped;

    public Store(final MeasurementStore wrapped) {
      LinkedBlockingQueue<MeasurementRegistrationInfo> q = new LinkedBlockingQueue<>();
      metrics = new AtomicReference<>(new  MeasurementCollector() {
        @Override
        public Collection<MeasurementRegistrationInfo> getCollection() {
          return q;
        }

        @Override
        public void accept(MeasurementRegistrationInfo t) {
          q.add(t);
        }
      });
      if (store == null) {
        store = this;
      } else {
        throw new IllegalStateException("Store singleton already set " + store);
      }
      this.wrapped = wrapped;
    }

    @Override
    public long alocateMeasurements(final MeasurementsInfo measurementInfo, final int sampleTimeMillis)
            throws IOException {
      long alocateMeasurements = wrapped.alocateMeasurements(measurementInfo, sampleTimeMillis);
      metrics.get().accept(new MeasurementRegistrationInfo(measurementInfo, sampleTimeMillis,
              alocateMeasurements));
      return alocateMeasurements;
    }

    public MeasurementCollector replace(final MeasurementCollector newValue) {
      return metrics.getAndSet(newValue);
    }

    @Override
    public void saveMeasurements(long tableId, long timeStampMillis, long... measurements) throws IOException {
      wrapped.saveMeasurements(tableId, timeStampMillis, measurements);
    }

    @Override
    public void flush() throws IOException {
      wrapped.flush();
    }

    @Override
    @Nullable
    public MeasurementStoreQuery query() {
      return wrapped.query();
    }

    @Override
    public void close() throws IOException {
      wrapped.close();
    }

  }

  private static class MetricDataSet implements AvroDataSet<IndexedRecord> {


    private final String name;

    private final Schema schema;

    private final int sampleTimeMillis;

    private final MetricsClusterResource clusterResource;

    public MetricDataSet(MeasurementRegistrationInfo info, MetricsClusterResource clusterResource) {
      this.name = info.mInfo.getMeasuredEntity().toString();
      schema = MetricsClusterResource.addNodeToSchema(
              TableDefs.createSchema(TableDefs.from(info.mInfo, info.sampleTimeMillis, info.registrationId)));
      sampleTimeMillis = info.sampleTimeMillis;
      this.clusterResource = clusterResource;
    }

    @Override
    public Schema getElementSchema() {
      return schema;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Set<AvroDataSet.Feature> getFeatures() {
      return Collections.EMPTY_SET;
    }

    @Override
    public long getRowCountStatistic() {
      return ManagementFactory.getRuntimeMXBean().getUptime() / sampleTimeMillis;
    }

    @Override
    public CloseableIterable<? extends IndexedRecord> getData(SqlPredicate<IndexedRecord> filter,
            List<String> selectProjections, AbacSecurityContext secCtx, long timeout, TimeUnit timeUnit) {
      try {
        return clusterResource.getClusterMetricsData(schema.getName(),
               null, null, Duration.ZERO);
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      } catch (URISyntaxException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

}
