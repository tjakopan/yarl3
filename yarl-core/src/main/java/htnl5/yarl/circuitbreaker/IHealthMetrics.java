package htnl5.yarl.circuitbreaker;

sealed interface IHealthMetrics permits RollingHealthMetrics, SingleHealthMetrics {
  void incrementSuccess();

  void incrementFailure();

  void reset();

  HealthCount getHealthCount();
}
