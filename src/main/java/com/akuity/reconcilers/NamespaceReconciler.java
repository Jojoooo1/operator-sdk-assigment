package com.akuity.reconcilers;

import static com.akuity.utils.Constants.NAMESPACE_IS_SYNC_LABEL_KEY;
import static com.akuity.utils.Constants.NAMESPACE_POD_SECURITY_ENFORCE_LABEL_KEY;
import static com.akuity.utils.Constants.NAMESPACE_POD_SECURITY_ENFORCE_VERSION_LABEL_KEY;
import static java.lang.String.format;

import com.akuity.conditions.HasValidNamespaceClass;
import com.akuity.conditions.HasValidNamespaceClassAndAdminRole;
import com.akuity.conditions.HasValidNamespaceClassAndEditorRole;
import com.akuity.conditions.HasValidNamespaceClassAndViewerRole;
import com.akuity.customresources.NamespaceClass;
import com.akuity.dependents.LimitRangeDR;
import com.akuity.dependents.NetworkPolicyDR;
import com.akuity.dependents.NetworkPolicyDenyAllByDefaultDR;
import com.akuity.dependents.ResourceQuotaDR;
import com.akuity.dependents.RoleAdminDR;
import com.akuity.dependents.RoleBindingAdminDR;
import com.akuity.dependents.RoleBindingEditorDR;
import com.akuity.dependents.RoleBindingViewerDR;
import com.akuity.dependents.RoleEditorDR;
import com.akuity.dependents.RoleViewerDR;
import com.akuity.dependents.ServiceAccountAdminDR;
import com.akuity.dependents.ServiceAccountEditorDR;
import com.akuity.dependents.ServiceAccountViewerDR;
import com.akuity.dependents.StorageClassDR;
import com.akuity.utils.NamespaceUtils;
import io.fabric8.kubernetes.api.model.Namespace;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowReconcileResult;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@CSVMetadata(
    bundleName = "NamespaceReconciler",
    provider = @CSVMetadata.Provider(name = "akuity.io"),
    description = "NamespaceReconciler definition")
@Slf4j
@ControllerConfiguration(
    name = "namespace-reconciler",
    dependents = {
      // Network DRs
      @Dependent(
          name = NetworkPolicyDenyAllByDefaultDR.COMPONENT,
          type = NetworkPolicyDenyAllByDefaultDR.class,
          reconcilePrecondition = HasValidNamespaceClass.class),
      @Dependent(
          name = NetworkPolicyDR.COMPONENT,
          type = NetworkPolicyDR.class,
          reconcilePrecondition = HasValidNamespaceClass.class),
      // Resource DRs
      @Dependent(
          name = LimitRangeDR.COMPONENT,
          type = LimitRangeDR.class,
          reconcilePrecondition = HasValidNamespaceClass.class),
      @Dependent(
          name = ResourceQuotaDR.COMPONENT,
          type = ResourceQuotaDR.class,
          reconcilePrecondition = HasValidNamespaceClass.class),
      // Storage DRs
      @Dependent(
          name = StorageClassDR.COMPONENT,
          type = StorageClassDR.class,
          reconcilePrecondition = HasValidNamespaceClass.class),
      // RBAC Viewer
      @Dependent(
          name = ServiceAccountViewerDR.COMPONENT,
          type = ServiceAccountViewerDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndViewerRole.class),
      @Dependent(
          name = RoleViewerDR.COMPONENT,
          type = RoleViewerDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndViewerRole.class),
      @Dependent(
          name = RoleBindingViewerDR.COMPONENT,
          type = RoleBindingViewerDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndViewerRole.class,
          dependsOn = {ServiceAccountViewerDR.COMPONENT, RoleViewerDR.COMPONENT}),
      // RBAC Editor
      @Dependent(
          name = ServiceAccountEditorDR.COMPONENT,
          type = ServiceAccountEditorDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndEditorRole.class),
      @Dependent(
          name = RoleEditorDR.COMPONENT,
          type = RoleEditorDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndEditorRole.class),
      @Dependent(
          name = RoleBindingEditorDR.COMPONENT,
          type = RoleBindingEditorDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndEditorRole.class,
          dependsOn = {ServiceAccountEditorDR.COMPONENT, RoleEditorDR.COMPONENT}),
      // RBAC Admin
      @Dependent(
          name = ServiceAccountAdminDR.COMPONENT,
          type = ServiceAccountAdminDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndAdminRole.class),
      @Dependent(
          name = RoleAdminDR.COMPONENT,
          type = RoleAdminDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndAdminRole.class),
      @Dependent(
          name = RoleBindingAdminDR.COMPONENT,
          type = RoleBindingAdminDR.class,
          reconcilePrecondition = HasValidNamespaceClassAndAdminRole.class,
          dependsOn = {ServiceAccountAdminDR.COMPONENT, RoleAdminDR.COMPONENT}),
    })
public class NamespaceReconciler
    implements Reconciler<Namespace>, EventSourceInitializer<Namespace> {

  protected static final Map<String, ResourceID> namespaceClassCache = new ConcurrentHashMap<>();

  @Override
  public UpdateControl<Namespace> reconcile(
      final Namespace primary, final Context<Namespace> context) {

    var nsClassInNamespaceOptional =
        NamespaceUtils.getNamespaceClassFromNamespaceOptional(context, primary);

    if (nsClassInNamespaceOptional.isEmpty()) {
      // Remove any managed labels if the namespace was managed by the operator.
      if (primary.getMetadata().getLabels().containsKey(NAMESPACE_IS_SYNC_LABEL_KEY)) {
        log.info("[{}] removing status and security labels", primary.getMetadata().getName());
        Namespace primaryUpdated = removeSecurityAndStatusLabelsFromNamespace(primary);
        return UpdateControl.updateResource(primaryUpdated);
      }

      return UpdateControl.noUpdate();
    }

    return context
        .managedDependentResourceContext()
        .getWorkflowReconcileResult()
        .map(
            result -> {
              log.info("[{}] adding status and security labels", primary.getMetadata().getName());
              Namespace primaryUpdated =
                  addSecurityAndStatusLabelsToNamespace(
                      primary, result, nsClassInNamespaceOptional.get());
              return UpdateControl.updateResourceAndPatchStatus(primaryUpdated);
            })
        .orElseThrow();
  }

  @Override
  public Map<String, EventSource> prepareEventSources(final EventSourceContext<Namespace> context) {

    // Watch all namespaces
    var nsES =
        new InformerEventSource<>(
            InformerConfiguration.from(Namespace.class, context).build(), context);

    // Watch all NamespaceClasses
    var nsClassES = buildNamespaceClassInformerEventSource(context);

    return EventSourceInitializer.nameEventSources(nsClassES, nsES);
  }

  private InformerEventSource<NamespaceClass, Namespace> buildNamespaceClassInformerEventSource(
      final EventSourceContext<Namespace> context) {
    return new InformerEventSource<>(
        InformerConfiguration.from(NamespaceClass.class, context)
            // There is no owner reference this is why we need to use the cache.
            .withPrimaryToSecondaryMapper(primary -> new HashSet<>(namespaceClassCache.values()))
            .withSecondaryToPrimaryMapper(
                secondary -> NamespaceUtils.buildNamespaceResourceIDsFromCache(context))
            .withOnAddFilter(
                secondary -> {
                  addToCache(secondary);
                  return true;
                })
            .withOnUpdateFilter(
                (oldSecondary, secondary) -> {
                  removeFromCache(oldSecondary);
                  addToCache(secondary);
                  return true;
                })
            .withOnDeleteFilter(
                (secondary, b) -> {
                  removeFromCache(secondary);
                  return true;
                })
            .build(),
        context);
  }

  private static Namespace addSecurityAndStatusLabelsToNamespace(
      final Namespace primary,
      final WorkflowReconcileResult result,
      final NamespaceClass nsClassInNamespace) {

    Map<String, String> nsLabels = new HashMap<>(primary.getMetadata().getLabels());
    nsLabels.put(NAMESPACE_POD_SECURITY_ENFORCE_VERSION_LABEL_KEY, "latest");
    nsLabels.put(
        NAMESPACE_POD_SECURITY_ENFORCE_LABEL_KEY,
        nsClassInNamespace.getSpec().getSecurity().getPodSecurityStandard().getProfile());

    if (result.allDependentResourcesReady()) {
      nsLabels.put(NAMESPACE_IS_SYNC_LABEL_KEY, Boolean.TRUE.toString());
    } else {
      nsLabels.put(NAMESPACE_IS_SYNC_LABEL_KEY, Boolean.FALSE.toString());
    }

    primary.getMetadata().setLabels(nsLabels);
    return primary;
  }

  private static Namespace removeSecurityAndStatusLabelsFromNamespace(final Namespace primary) {
    Map<String, String> nsLabels = new HashMap<>(primary.getMetadata().getLabels());
    nsLabels.remove(NAMESPACE_POD_SECURITY_ENFORCE_VERSION_LABEL_KEY);
    nsLabels.remove(NAMESPACE_POD_SECURITY_ENFORCE_LABEL_KEY);
    nsLabels.remove(NAMESPACE_IS_SYNC_LABEL_KEY);
    primary.getMetadata().setLabels(nsLabels);
    return primary;
  }
  
  private static void addToCache(final ResourceID resourceID) {
    if (namespaceClassCache.containsKey(resourceID.getName())) {
      var errorMessage =
          format(
              "NamespaceClass with name '%s' already exist, please remove one of the CRDs",
              resourceID.getName());
      log.error(errorMessage);
      // Send message to slack!!
      throw new IllegalStateException(errorMessage);
    }
    namespaceClassCache.put(resourceID.getName(), resourceID);
  }

  private static void removeFromCache(final ResourceID resourceID) {
    namespaceClassCache.remove(resourceID.getName(), resourceID);
  }

  private static void addToCache(final NamespaceClass namespaceClass) {
    addToCache(
        new ResourceID(
            namespaceClass.getMetadata().getName(), namespaceClass.getMetadata().getNamespace()));
  }

  private static void removeFromCache(final NamespaceClass namespaceClass) {
    removeFromCache(
        new ResourceID(
            namespaceClass.getMetadata().getName(), namespaceClass.getMetadata().getNamespace()));
  }
}
