package htnl5.yarl;

// producer of B
public interface IFluentPolicyBuilder<B extends IFluentPolicyBuilder<B>> {
  B self();
}
