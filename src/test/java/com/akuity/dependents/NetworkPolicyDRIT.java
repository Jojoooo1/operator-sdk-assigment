package com.akuity.dependents;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.akuity.KubernetesDependentResourceBaseTest;
import com.akuity.customresources.NamespaceClass;
import com.akuity.reconcilers.NamespaceReconciler;
import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.networking.v1.IPBlockBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeerBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import java.util.List;
import org.junit.jupiter.api.extension.RegisterExtension;

class NetworkPolicyDRIT extends KubernetesDependentResourceBaseTest<NetworkPolicy> {

  @RegisterExtension
  static LocallyRunOperatorExtension operator =
      LocallyRunOperatorExtension.builder()
          .waitForNamespaceDeletion(true)
          // Necessary to keep namespace name small, or it will exceed 63 characters.
          .withPerClassNamespaceNameSupplier(extensionContext -> random(6))
          .oneNamespacePerClass(true)
          .withReconciler(new NamespaceReconciler())
          .withKubernetesClient(new KubernetesClientBuilder().build())
          .withAdditionalCustomResourceDefinition(NamespaceClass.class)
          .build();

  @Override
  public LocallyRunOperatorExtension operator() {
    return operator;
  }

  @Override
  public Class<NetworkPolicy> getDependentResourceClass() {
    return NetworkPolicy.class;
  }

  @Override
  public String getResourceName() {
    return ResourceUtils.name(operator.getNamespace(), NetworkPolicyDR.COMPONENT);
  }

  @Override
  public void dependentResourceAssertion(NetworkPolicy resource, NamespaceClass nsClass) {
    assertThat(resource.getSpec().getEgress().get(0).getTo().get(0).getIpBlock().getCidr())
        .isEqualTo(nsClass.getSpec().getNetworkPolicy().getEgress());
  }

  @Override
  public NetworkPolicy getDependentResourceToUpdate() {
    var resourceToUpdate = operator().get(getDependentResourceClass(), getResourceName());
    resourceToUpdate
        .getSpec()
        .getEgress()
        .get(0)
        .setTo(
            List.of(
                new NetworkPolicyPeerBuilder()
                    .withIpBlock(new IPBlockBuilder().withCidr("1.2.3.4/1").build())
                    .build()));
    return resourceToUpdate;
  }
}
