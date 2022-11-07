package htnl5.yarl;

import java.util.*;

public final class Context implements Map<String, Object> {
  private final String operationKey;
  private String policyWrapKey;
  private String policyKey;
  private UUID correlationId;
  private final Map<String, Object> contextData;

  public Context(final String operationKey, final Map<String, Object> contextData) {
    this.operationKey = operationKey;
    this.contextData = contextData;
  }

  public Context() {
    this(null, new HashMap<>());
  }

  public Context(final String operationKey) {
    this(operationKey, new HashMap<>());
  }

  Context(final Map<String, Object> contextData) {
    this(null, contextData);
  }

  static Context none() {
    return new Context();
  }

  public Optional<String> getOperationKey() {
    return Optional.ofNullable(operationKey);
  }

  public Optional<String> getPolicyWrapKey() {
    return Optional.ofNullable(policyWrapKey);
  }

  // internal
  public void setPolicyWrapKey(final String policyWrapKey) {
    this.policyWrapKey = policyWrapKey;
  }

  public Optional<String> getPolicyKey() {
    return Optional.ofNullable(policyKey);
  }

  void setPolicyKey(final String policyKey) {
    this.policyKey = policyKey;
  }

  public UUID getCorrelationId() {
    if (correlationId == null) {
      correlationId = UUID.randomUUID();
    }
    return correlationId;
  }

  @Override
  public int size() {
    return contextData.size();
  }

  @Override
  public boolean isEmpty() {
    return contextData.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    return contextData.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    return contextData.containsValue(value);
  }

  @Override
  public Object get(final Object key) {
    return contextData.get(key);
  }

  @Override
  public Object put(final String key, final Object value) {
    return contextData.put(key, value);
  }

  @Override
  public Object remove(final Object key) {
    return contextData.remove(key);
  }

  @Override
  public void putAll(final Map<? extends String, ?> m) {
    contextData.putAll(m);
  }

  @Override
  public void clear() {
    contextData.clear();
  }

  @Override
  public Set<String> keySet() {
    return contextData.keySet();
  }

  @Override
  public Collection<Object> values() {
    return contextData.values();
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    return contextData.entrySet();
  }
}
