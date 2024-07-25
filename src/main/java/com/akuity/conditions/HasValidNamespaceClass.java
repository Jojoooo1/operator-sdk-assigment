package com.akuity.conditions;

import static com.akuity.utils.ConditionUtils.hasValidNamespaceClass;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HasValidNamespaceClass<R extends HasMetadata> implements Condition<R, Namespace> {

  @Override
  public boolean isMet(
      final DependentResource<R, Namespace> dependentResource,
      final Namespace primary,
      final Context<Namespace> context) {

    return hasValidNamespaceClass(dependentResource, primary, context, true);
  }
}
