package org.apache.commons.codec;

public interface BinaryEncoder extends Encoder {

    byte[] encode(byte[] abyte) throws EncoderException;
}
