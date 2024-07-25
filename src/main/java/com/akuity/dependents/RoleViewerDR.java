package com.akuity.dependents;

import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;

import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/*
 *
 * This role permits unlimited read/write access to resources within a namespace.
 * This role can create roles and role bindings within a particular namespace.
 * It does not permit write access to the namespace itself.
 * */
@Slf4j
@KubernetesDependent(
    labelSelector = K8S_NAMESPACE_CLASS,
    resourceDiscriminator = RoleViewerDR.Discriminator.class)
public class RoleViewerDR extends CRUDKubernetesDependentResource<Role, Namespace>
    implements SecondaryToPrimaryMapper<Role> {

  public static final String COMPONENT = "role-viewer";

  public RoleViewerDR() {
    super(Role.class);
  }

  @Override
  public Role desired(final Namespace primary, final Context<Namespace> context) {
    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();
    return new RoleBuilder()
        .withMetadata(meta)
        .addNewRule()
        .withApiGroups("")
        .withResources("pods", "services", "configmaps", "persistentvolumeclaims")
        .withVerbs("get", "list", "watch")
        .endRule()
        .addNewRule()
        .withApiGroups("apps")
        .withResources("deployments", "replicasets", "statefulsets")
        .withVerbs("get", "list", "watch")
        .endRule()
        .addNewRule()
        .withApiGroups("batch")
        .withResources("jobs", "cronjobs")
        .withVerbs("get", "list", "watch")
        .endRule()
        .build();
  }

  /*
   *  Necessary to map secondary resources to primary resource.
   *  It only uses the namespace name to have consistent mapping.
   * */
  @Override
  public Set<ResourceID> toPrimaryResourceIDs(final Role secondary) {
    return Set.of(new ResourceID(secondary.getMetadata().getNamespace()));
  }

  /*
   *  Necessary since the controller is managing multiple k8s resources
   * */
  public static class Discriminator extends ResourceIDMatcherDiscriminator<Role, Namespace> {
    public Discriminator() {
      super(
          COMPONENT,
          primary ->
              new ResourceID(
                  ResourceUtils.name(primary, COMPONENT), ResourceUtils.namespace(primary)));
    }
  }
}
