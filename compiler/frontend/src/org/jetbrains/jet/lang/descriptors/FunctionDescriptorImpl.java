package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.DescriptorSubstitutor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public abstract class FunctionDescriptorImpl extends DeclarationDescriptorImpl implements FunctionDescriptor {

    protected List<TypeParameterDescriptor> typeParameters;
    protected List<ValueParameterDescriptor> unsubstitutedValueParameters;
    protected JetType unsubstitutedReturnType;
    private ReceiverDescriptor receiver;
    protected ReceiverDescriptor expectedThisObject;

    protected Modality modality;
    protected Visibility visibility;
    private final Set<FunctionDescriptor> overriddenFunctions = Sets.newLinkedHashSet();
    private final FunctionDescriptor original;

    protected FunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name) {
        super(containingDeclaration, annotations, name);
        this.original = this;
    }

    protected FunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containigDeclaration,
            @NotNull FunctionDescriptor original,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name) {
        super(containigDeclaration, annotations, name);
        this.original = original;
    }

    public FunctionDescriptorImpl initialize(
            @Nullable JetType receiverType,
            @NotNull ReceiverDescriptor expectedThisObject,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable JetType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility) {
        this.typeParameters = typeParameters;
        this.unsubstitutedValueParameters = unsubstitutedValueParameters;
        this.unsubstitutedReturnType = unsubstitutedReturnType;
        this.modality = modality;
        this.visibility = visibility;
        this.receiver = receiverType == null ? NO_RECEIVER : new ExtensionReceiver(this, receiverType);
        this.expectedThisObject = expectedThisObject;
        return this;
    }

    public void setReturnType(@NotNull JetType unsubstitutedReturnType) {
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getReceiverParameter() {
        return receiver;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getExpectedThisObject() {
        return expectedThisObject;
    }

    @NotNull
    @Override
    public Set<? extends FunctionDescriptor> getOverriddenDescriptors() {
        return overriddenFunctions;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    public void addOverriddenFunction(@NotNull FunctionDescriptor overriddenFunction) {
        overriddenFunctions.add(overriddenFunction);
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParameters;
    }

    @Override
    @NotNull
    public List<ValueParameterDescriptor> getValueParameters() {
        return unsubstitutedValueParameters;
    }

    @Override
    public JetType getReturnType() {
        return unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return original == this ? this : original.getOriginal();
    }

    @Override
    public final FunctionDescriptor substitute(TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }
        return doSubstitute(originalSubstitutor, getContainingDeclaration(), modality, true);
    }

    protected FunctionDescriptor doSubstitute(TypeSubstitutor originalSubstitutor,
            DeclarationDescriptor newOwner, Modality newModality, boolean preserveOriginal) {
        FunctionDescriptorImpl substitutedDescriptor = createSubstitutedCopy(newOwner, preserveOriginal);

        List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

        JetType substitutedReceiverType = null;
        if (receiver.exists()) {
            substitutedReceiverType = substitutor.substitute(getReceiverParameter().getType(), Variance.IN_VARIANCE);
            if (substitutedReceiverType == null) {
                return null;
            }
        }

        ReceiverDescriptor substitutedExpectedThis = NO_RECEIVER;
        if (expectedThisObject.exists()) {
            JetType substitutedType = substitutor.substitute(expectedThisObject.getType(), Variance.INVARIANT);
            if (substitutedType == null) {
                return null;
            }
            substitutedExpectedThis = new TransientReceiver(substitutedType);
        }

        List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorUtil.getSubstitutedValueParameters(substitutedDescriptor, this, substitutor);
        if (substitutedValueParameters == null) {
            return null;
        }

        JetType substitutedReturnType = FunctionDescriptorUtil.getSubstitutedReturnType(this, substitutor);
        if (substitutedReturnType == null) {
            return null;
        }

        substitutedDescriptor.initialize(
                substitutedReceiverType,
                substitutedExpectedThis,
                substitutedTypeParameters,
                substitutedValueParameters,
                substitutedReturnType,
                newModality, 
                visibility
        );
        for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
            substitutedDescriptor.addOverriddenFunction(overriddenFunction);
        }
        return substitutedDescriptor;
    }

    protected abstract FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal);

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionDescriptor(this, data);
    }
}
