package com.akuity.dependents;

import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;

import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
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
 * This role allows read-only access within a given namespace.
 * It does not allow viewing or modifying of roles or role bindings.
 * */
@Slf4j
@KubernetesDependent(
    labelSelector = K8S_NAMESPACE_CLASS,
    resourceDiscriminator = ServiceAccountViewerDR.Discriminator.class)
public class ServiceAccountViewerDR
    extends CRUDKubernetesDependentResource<ServiceAccount, Namespace>
    implements SecondaryToPrimaryMapper<ServiceAccount> {

  public static final String COMPONENT = "sa-viewer";

  public ServiceAccountViewerDR() {
    super(ServiceAccount.class);
  }

  @Override
  public ServiceAccount desired(final Namespace primary, final Context<Namespace> context) {
    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();
    return new ServiceAccountBuilder().withMetadata(meta).build();
  }

  /*
   *  Necessary to map secondary resources to primary resource.
   *  It only uses the namespace name to have consistent mapping.
   * */
  @Override
  public Set<ResourceID> toPrimaryResourceIDs(final ServiceAccount secondary) {
    return Set.of(new ResourceID(secondary.getMetadata().getNamespace()));
  }

  /*
   *  Necessary since the controller is managing multiple k8s resources
   * */
  public static class Discriminator
      extends ResourceIDMatcherDiscriminator<ServiceAccount, Namespace> {
    public Discriminator() {
      super(
          COMPONENT,
          primary ->
              new ResourceID(
                  ResourceUtils.name(primary, COMPONENT), ResourceUtils.namespace(primary)));
    }
  }
}
