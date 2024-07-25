package com.akuity.conditions;

import static com.akuity.utils.ConditionUtils.hasAllowedRole;
import static com.akuity.utils.ConditionUtils.hasValidNamespaceClass;

import com.akuity.customresources.NamespaceClassSpec.RoleEnum;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HasValidNamespaceClassAndEditorRole<R extends HasMetadata>
    implements Condition<R, Namespace> {

  @Override
  public boolean isMet(
      final DependentResource<R, Namespace> dependentResource,
      final Namespace primary,
      final Context<Namespace> context) {

    // 1. verify namespaceClassIsValid
    if (!hasValidNamespaceClass(dependentResource, primary, context, false)) return false;

    // 2. Verify editor role is passed to the CRD
    return hasAllowedRole(dependentResource, primary, context, RoleEnum.EDITOR);
  }
}
