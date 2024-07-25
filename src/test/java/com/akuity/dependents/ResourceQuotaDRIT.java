package com.akuity.dependents;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.akuity.KubernetesDependentResourceBaseTest;
import com.akuity.customresources.NamespaceClass;
import com.akuity.reconcilers.NamespaceReconciler;
import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

class ResourceQuotaDRIT extends KubernetesDependentResourceBaseTest<ResourceQuota> {

  @RegisterExtension
  static LocallyRunOperatorExtension operator =
      LocallyRunOperatorExtension.builder()
          .waitForNamespaceDeletion(false)
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
  public Class<ResourceQuota> getDependentResourceClass() {
    return ResourceQuota.class;
  }

  @Override
  public String getResourceName() {
    return ResourceUtils.name(operator.getNamespace(), ResourceQuotaDR.COMPONENT);
  }

  @Override
  public void dependentResourceAssertion(ResourceQuota resource, NamespaceClass nsClass) {
    assertThat(resource.getSpec().getHard().get("requests.cpu").getAmount())
        .isEqualTo(nsClass.getSpec().getResourceQuota().getCpu());
  }

  @Override
  public ResourceQuota getDependentResourceToUpdate() {
    var resourceToUpdate = operator().get(getDependentResourceClass(), getResourceName());
    resourceToUpdate.getSpec().getHard().get("requests.cpu").setAmount("100m");
    return resourceToUpdate;
  }
}
