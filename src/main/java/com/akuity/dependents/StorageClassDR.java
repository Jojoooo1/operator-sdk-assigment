package com.akuity.dependents;

import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;

import com.akuity.utils.NamespaceUtils;
import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@KubernetesDependent(
    labelSelector = K8S_NAMESPACE_CLASS,
    resourceDiscriminator = StorageClassDR.Discriminator.class)
public class StorageClassDR extends CRUDKubernetesDependentResource<StorageClass, Namespace>
    implements SecondaryToPrimaryMapper<StorageClass> {

  public static final String COMPONENT = "storage-class";

  public StorageClassDR() {
    super(StorageClass.class);
  }

  @Override
  public StorageClass desired(final Namespace primary, final Context<Namespace> context) {

    var namespaceClass =
        NamespaceUtils.getNamespaceClassFromNamespaceOrThrowException(context, primary);
    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();

    return new StorageClassBuilder()
        .withMetadata(meta)
        .withProvisioner(namespaceClass.getSpec().getStorage().getProvisioner())
        // delays the binding and provisioning of a PV until a pod that uses the PVC is created
        .withVolumeBindingMode("WaitForFirstConsumer")
        // allows for manual reclamation of the resource.
        .withReclaimPolicy("Retain")
        .build();
  }

  /*
   *  Necessary to map secondary resources to primary resource.
   *  It only uses the namespace name to have consistent mapping.
   * */
  @Override
  public Set<ResourceID> toPrimaryResourceIDs(final StorageClass secondary) {
    return Set.of(
        new ResourceID(StringUtils.remove(secondary.getMetadata().getName(), COMPONENT + "-")));
  }

  /*
   *  Necessary since the controller is managing multiple k8s resources
   * */
  public static class Discriminator
      extends ResourceIDMatcherDiscriminator<StorageClass, Namespace> {
    public Discriminator() {
      super(COMPONENT, primary -> new ResourceID(ResourceUtils.name(primary, COMPONENT)));
    }
  }
}
