
package org.spf4j.actuator.health;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.spf4j.base.ExecutionContext;
import org.spf4j.base.ExecutionContexts;
import org.spf4j.base.avro.HealthRecord;
import org.spf4j.base.avro.HealthStatus;

/**
 * @author Zoltan Farkas
 */
public interface HealthCheck {

    enum Type {local, cluster, custom};


    public interface Registration {
      String[] getPath();

      HealthCheck getCheck();
    }


    /**
     * Will run the health check.
     * If health check is supposed to fail
     * @param logger
     * @throws Exception
     */
    void test(final Logger logger) throws Exception;

    default HealthRecord getRecord(final String name, final String origin, final Logger logger,
            final boolean isDebug, final boolean isDebugOnError) {
      try (ExecutionContext ec = ExecutionContexts.start(name,
               timeout(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS)) {
         HealthRecord.Builder respBuilder = HealthRecord.newBuilder()
                 .setName(name);
         try {
           test(logger);
           respBuilder.setStatus(HealthStatus.HEALTHY);
           if (isDebug) {
             respBuilder.setDetail(ec.getDebugDetail(origin, null));
           }
         } catch (Exception ex) {
           respBuilder.setStatus(HealthStatus.UNHEALTHY);
           if (isDebugOnError) {
             respBuilder.setDetail(ec.getDebugDetail(origin, ex));
           }
         }
         respBuilder.setOrigin(origin);
         return respBuilder.build();
       }
    }


    String info();

    default Type getType() {
      return Type.local;
    }

    default long timeout(final TimeUnit tu) {
      return tu.convert(10, TimeUnit.SECONDS);
    }

    HealthCheck NOP = new HealthCheck() {
      @Override
      public void test(final Logger logger) throws Exception {
      }

      @Override
      public String info() {
        return "This is a health check that does nothing";
      }

      @Override
      public String toString() {
        return "NOP";
      }

    };
}
