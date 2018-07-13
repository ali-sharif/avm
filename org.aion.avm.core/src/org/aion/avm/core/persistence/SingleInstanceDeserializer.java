package org.aion.avm.core.persistence;

import org.aion.avm.internal.IObjectDeserializer;


/**
 * One of these objects is created for each object instance we are deserializing.
 * It is basically a thin wrapper over StreamingPrimitiveCodec.Decoder.
 */
public class SingleInstanceDeserializer implements IObjectDeserializer {
    private final IAutomatic automaticPart;
    private final StreamingPrimitiveCodec.Decoder decoder;

    public SingleInstanceDeserializer(IAutomatic automaticPart, StreamingPrimitiveCodec.Decoder decoder) {
        this.automaticPart = automaticPart;
        this.decoder = decoder;
    }

    @Override
    public void beginAutomatically(org.aion.avm.shadow.java.lang.Object instance, Class<?> firstManualClass) {
        this.automaticPart.partialAutomaticDeserializeInstance(this.decoder, instance, firstManualClass);
    }

    @Override
    public int readInt() {
        return this.decoder.decodeInt();
    }


    /**
     * This is the interface we must be given to handle the "automatic" part of the deserialization.
     * This has to come through us since we own the decoder.
     */
    public static interface IAutomatic {
        /**
         * Requests that the given instance be partially automatically deserialized:  the receiver is responsible for automatic deserialization of
         * all field defined between (exclusive) the shadow Object and firstManualClass as shadow Object provides special handling for its fields
         * and firstManualClass (and all sub-classes) manually deserialize their fields.
         * 
         * @param decoder The decoder to use.
         * @param instance The object instance to deserialize.
         * @param firstManualClass This class, and all sub-classes, will manually deserialize their declared fields (if null, the entire object is automatic).
         */
        void partialAutomaticDeserializeInstance(StreamingPrimitiveCodec.Decoder decoder, org.aion.avm.shadow.java.lang.Object instance, Class<?> firstManualClass);
    }
}