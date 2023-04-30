package htnl5.yarl;

// producer of P
public interface IBuildable<P extends IPolicy> {
  P build();
}
