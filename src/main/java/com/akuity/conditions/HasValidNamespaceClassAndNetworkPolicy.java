package com.akuity.conditions;

import static com.akuity.utils.ConditionUtils.hasValidNamespaceClass;

import com.akuity.utils.NamespaceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HasValidNamespaceClassAndNetworkPolicy<R extends HasMetadata>
    implements Condition<R, Namespace> {

  @Override
  public boolean isMet(
      final DependentResource<R, Namespace> dependentResource,
      final Namespace primary,
      final Context<Namespace> context) {

    // 1. verify namespaceClassIsValid
    if (!hasValidNamespaceClass(dependentResource, primary, context, false)) return false;

    // 2. Verify networkPolicy is passed
    return hasNetworkPolicy(dependentResource, primary, context);
  }

  private static <R extends HasMetadata> boolean hasNetworkPolicy(
      final DependentResource<R, Namespace> dependentResource,
      final Namespace primary,
      final Context<Namespace> context) {

    var namespaceClass = NamespaceUtils.getNamespaceClassFromNamespaceOptional(context, primary);
    if (namespaceClass.isEmpty()
        || namespaceClass.get().getSpec() == null
        || namespaceClass.get().getSpec().getNetworkPolicy() == null) {
      log.info(
          "namespace '{}' '{}' no networkPolicy found in spec, removing dependent if exist.",
          primary.getMetadata().getName(),
          dependentResource.getClass().getSimpleName());
      return false;
    }

    log.info(
        "[{}][{}] networkPolicy found in spec, starting reconciliation.",
        primary.getMetadata().getName(),
        dependentResource.getClass().getSimpleName());

    return true;
  }
}
