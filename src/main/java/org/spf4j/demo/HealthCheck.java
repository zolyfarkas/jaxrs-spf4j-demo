
package org.spf4j.demo;

/**
 * @author Zoltan Farkas
 */
public interface HealthCheck {

    public interface Registration {
      String[] getPath();

      HealthCheck getCheck();
    }


    public interface Result {
      boolean isHealthy();

      String getErrorMessage();

      Exception getValidationException();


      Result HEALTHY = new Result() {
        @Override
        public boolean isHealthy() {
          return true;
        }

        @Override
        public String getErrorMessage() {
          throw new UnsupportedOperationException();
        }

        @Override
        public Exception getValidationException() {
          return null;
        }
      };

      static Result unhealthy(String errorMessage) {
        return new Result() {
          @Override
          public boolean isHealthy() {
            return false;
          }

          @Override
          public String getErrorMessage() {
            return errorMessage;
          }

          @Override
          public Exception getValidationException() {
            return null;
          }

        };
      }

      static Result unhealthy(final String errorMessage, final Exception error) {
        return new Result() {
          @Override
          public boolean isHealthy() {
            return false;
          }

          @Override
          public String getErrorMessage() {
            return errorMessage;
          }

          @Override
          public Exception getValidationException() {
            return error;
          }

        };
      }

    }


    Result test();


    HealthCheck ALWAYS_HEALTHY = () -> Result.HEALTHY;


    default HealthCheck and(HealthCheck check) {
      return () -> {
        Result test = this.test();
        if (test.isHealthy()) {
          return check.test();
        } else {
          return test;
        }
      };
    }

}
