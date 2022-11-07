package htnl5.yarl.wrap;

import htnl5.yarl.IPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface IPolicyWrap extends IPolicy {
  IPolicy getOuter();

  IPolicy getInner();

  default Iterable<IPolicy> getAllPolicies() {
    final var policies = new ArrayList<IPolicy>();
    final var childPolicies = List.of(getOuter(), getInner());
    for (final IPolicy childPolicy : childPolicies) {
      if (childPolicy instanceof IPolicyWrap wrap) {
        for (final IPolicy policy : wrap.getAllPolicies()) {
          policies.add(policy);
        }
      } else {
        policies.add(childPolicy);
      }
    }
    return policies;
  }

  default Iterable<IPolicy> getPolicies(final Class<? extends IPolicy> policyClass) {
    return getAllPoliciesStream()
      .filter(policyClass::isInstance)
      .toList();
  }

  default <P extends IPolicy> Iterable<IPolicy> getPolicies(final Class<? extends P> policyClass,
                                                            final Predicate<? super P> predicate) {
    //noinspection unchecked
    return getAllPoliciesStream()
      .filter(p -> policyClass.isInstance(p) && predicate.test((P) p))
      .toList();
  }

  default Optional<IPolicy> getPolicy(final Class<? extends IPolicy> policyClass) {
    return getAllPoliciesStream()
      .filter(policyClass::isInstance)
      .findFirst();
  }

  default <P extends IPolicy> Optional<IPolicy> getPolicy(final Class<? extends IPolicy> policyClass,
                                                          final Predicate<? super P> predicate) {
    //noinspection unchecked
    return getAllPoliciesStream()
      .filter(p -> policyClass.isInstance(p) && predicate.test((P) p))
      .findFirst();
  }

  private Stream<IPolicy> getAllPoliciesStream() {
    return StreamSupport.stream(getAllPolicies().spliterator(), false);
  }
}
