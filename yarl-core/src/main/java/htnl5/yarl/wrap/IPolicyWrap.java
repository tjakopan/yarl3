package htnl5.yarl.wrap;

import htnl5.yarl.IPolicy;

import java.util.*;
import java.util.function.Predicate;

public interface IPolicyWrap extends IPolicy {
  IPolicy getOuter();

  IPolicy getInner();

  default List<IPolicy> getAllPolicies() {
    final var policies = new ArrayList<IPolicy>();
    final var childPolicies = List.of(getOuter(), getInner());
    for (final IPolicy childPolicy : childPolicies) {
      if (childPolicy instanceof IPolicyWrap wrap) {
        policies.addAll(wrap.getAllPolicies());
      } else {
        policies.add(childPolicy);
      }
    }
    return Collections.unmodifiableList(policies);
  }

  default List<IPolicy> getPolicies(final Class<? extends IPolicy> policyClass) {
    Objects.requireNonNull(policyClass, "policyClass must not be null.");
    return getAllPolicies().stream()
      .filter(policyClass::isInstance)
      .toList();
  }

  default <P extends IPolicy> List<IPolicy> getPolicies(final Class<? extends P> policyClass,
                                                        final Predicate<? super P> predicate) {
    Objects.requireNonNull(policyClass, "policyClass must not be null.");
    Objects.requireNonNull(predicate, "predicate must not be null.");
    //noinspection unchecked
    return getAllPolicies().stream()
      .filter(p -> policyClass.isInstance(p) && predicate.test((P) p))
      .toList();
  }

  default Optional<IPolicy> getPolicy(final Class<? extends IPolicy> policyClass) {
    Objects.requireNonNull(policyClass, "policyClass must not be null.");
    return getAllPolicies().stream()
      .filter(policyClass::isInstance)
      .reduce((a, b) -> {
        throw new IllegalStateException("Policies contain multiple policies of class %s.".formatted(policyClass.getSimpleName()));
      });
  }

  default <P extends IPolicy> Optional<IPolicy> getPolicy(final Class<? extends P> policyClass,
                                                          final Predicate<? super P> predicate) {
    Objects.requireNonNull(policyClass, "policyClass must not be null.");
    Objects.requireNonNull(predicate, "predicate must not be null.");
    //noinspection unchecked
    return getAllPolicies().stream()
      .filter(p -> policyClass.isInstance(p) && predicate.test((P) p))
      .reduce((a, b) -> {
        throw new IllegalStateException(("Policies contain multiple policies of class %s that fulfill the " +
          "predicate").formatted(policyClass.getSimpleName()));
      });
  }
}
