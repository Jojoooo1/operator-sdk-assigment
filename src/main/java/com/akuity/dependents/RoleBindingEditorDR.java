package com.akuity.dependents;

import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;

import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
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
    resourceDiscriminator = RoleBindingEditorDR.Discriminator.class)
public class RoleBindingEditorDR extends CRUDKubernetesDependentResource<RoleBinding, Namespace>
    implements SecondaryToPrimaryMapper<RoleBinding> {

  public static final String COMPONENT = "role-binding-editor";

  public RoleBindingEditorDR() {
    super(RoleBinding.class);
  }

  @Override
  public RoleBinding desired(final Namespace primary, final Context<Namespace> context) {
    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();

    return new RoleBindingBuilder()
        .withMetadata(meta)
        .addNewSubject()
        .withKind("ServiceAccount")
        .withName(ResourceUtils.name(primary, ServiceAccountEditorDR.COMPONENT))
        .withNamespace(primary.getMetadata().getName())
        .endSubject()
        .withNewRoleRef()
        .withKind("Role")
        .withName(ResourceUtils.name(primary, RoleEditorDR.COMPONENT))
        .withApiGroup("rbac.authorization.k8s.io")
        .endRoleRef()
        .build();
  }

  /*
   *  Necessary to map secondary resources to primary resource.
   *  It only uses the namespace name to have consistent mapping.
   * */
  @Override
  public Set<ResourceID> toPrimaryResourceIDs(final RoleBinding secondary) {
    return Set.of(new ResourceID(secondary.getMetadata().getNamespace()));
  }

  /*
   *  Necessary since the controller is managing multiple k8s resources
   * */
  public static class Discriminator extends ResourceIDMatcherDiscriminator<RoleBinding, Namespace> {
    public Discriminator() {
      super(
          COMPONENT,
          primary ->
              new ResourceID(
                  ResourceUtils.name(primary, COMPONENT), ResourceUtils.namespace(primary)));
    }
  }
}
