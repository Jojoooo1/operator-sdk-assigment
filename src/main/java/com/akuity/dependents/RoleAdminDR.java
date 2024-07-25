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

@Slf4j
@KubernetesDependent(
    labelSelector = K8S_NAMESPACE_CLASS,
    resourceDiscriminator = RoleAdminDR.Discriminator.class)
public class RoleAdminDR extends CRUDKubernetesDependentResource<Role, Namespace>
    implements SecondaryToPrimaryMapper<Role> {

  public static final String COMPONENT = "role-admin";

  public RoleAdminDR() {
    super(Role.class);
  }

  @Override
  public Role desired(final Namespace primary, final Context<Namespace> context) {
    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();
    return new RoleBuilder()
        .withMetadata(meta)
        .addNewRule()
        .withApiGroups("*")
        .withResources("*")
        .withVerbs("*")
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
