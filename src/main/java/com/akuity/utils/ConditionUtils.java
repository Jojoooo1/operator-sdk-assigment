package com.akuity.utils;

import static com.akuity.utils.Constants.NAMESPACE_IS_SYNC_LABEL_KEY;
import static com.akuity.utils.NamespaceUtils.getNsClassLabelFromNamespaceOrEmptyString;
import static java.lang.String.format;

import com.akuity.customresources.NamespaceClassSpec.RoleEnum;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ConditionUtils {

  public static <R extends HasMetadata> boolean hasValidNamespaceClass(
      final DependentResource<R, Namespace> dependentResource,
      final Namespace primary,
      final Context<Namespace> context,
      final boolean isLastValidation) {

    var namespaceName = primary.getMetadata().getName();
    var nsClassInNamespaceOptional =
        NamespaceUtils.getNamespaceClassFromNamespaceOptional(context, primary);

    if (nsClassInNamespaceOptional.isEmpty()) {
      var logMessage =
          format(
              "[%s] namespace does not have any valid namespaceClass label, removing dependent if exist.",
              namespaceName);
      if (primary.getMetadata().getLabels().containsKey(NAMESPACE_IS_SYNC_LABEL_KEY)) {
        log.info(logMessage);
      } else {
        log.debug(logMessage);
      }
      return false;
    }

    if (isLastValidation) {
      log.info(
          "[{}][{}] has valid namespaceClass label '{}', starting reconciliation.",
          primary.getMetadata().getName(),
          dependentResource.getClass().getSimpleName(),
          nsClassInNamespaceOptional.get().getMetadata().getName());
    } else {
      log.debug(
          "[{}][{}] has valid namespaceClass label '{}'",
          namespaceName,
          dependentResource.getClass().getSimpleName(),
          getNsClassLabelFromNamespaceOrEmptyString(primary));
    }

    return true;
  }

  public static <R extends HasMetadata> boolean hasAllowedRole(
      final DependentResource<R, Namespace> dependentResource,
      final Namespace primary,
      final Context<Namespace> context,
      RoleEnum roleEnum) {

    var namespaceClass = NamespaceUtils.getNamespaceClassFromNamespaceOptional(context, primary);
    if (namespaceClass.isEmpty()
        || namespaceClass.get().getSpec() == null
        || namespaceClass.get().getSpec().getRbac() == null
        || namespaceClass.get().getSpec().getRbac().getAllowedRoles() == null
        || namespaceClass.get().getSpec().getRbac().getAllowedRoles().stream()
            .noneMatch(role -> roleEnum.getRole().equals(role.getRole()))) {
      log.info(
          "namespace '{}' '{}' no allowedRoles with value '{}' found in rbac, removing dependent if exist.",
          primary.getMetadata().getName(),
          dependentResource.getClass().getSimpleName(),
          roleEnum.getRole());
      return false;
    }

    log.info(
        "[{}][{}] allowedRoles with value '{}' found in spec, starting reconciliation.",
        primary.getMetadata().getName(),
        dependentResource.getClass().getSimpleName(),
        roleEnum.getRole());

    return true;
  }
}
