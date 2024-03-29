package org.metadatacenter.cedar.api.constraints;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-07-26
 */
public record LiteralValueConstraint(String label, boolean selectedByDefault) {

    public static LiteralValueConstraint of(String label, boolean defaultValue) {
        return new LiteralValueConstraint(label, defaultValue);
    }
}

