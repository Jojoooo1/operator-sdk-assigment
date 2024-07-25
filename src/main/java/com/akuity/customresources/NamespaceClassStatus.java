package com.akuity.customresources;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NamespaceClassStatus extends ObservedGenerationAwareStatus {}
