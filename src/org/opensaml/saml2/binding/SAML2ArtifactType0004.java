/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.saml2.binding;

import java.util.Arrays;

/**
 * SAML 2 Type 0x004 Artifact. SAML 2, type 4, artifacts contains a 2 byte type code with a value of 4 follwed by a 2
 * byte endpoint index followed by a 20 byte source ID followed by a 20 byte message handle.
 */
public class SAML2ArtifactType0004 extends SAML2Artifact {

    /** SAML 2 artifact type code (0x0004) */
    public final static byte[] TYPE_CODE = { 0, 4 };

    /** 20 byte artifact source ID */
    private byte[] sourceID;

    /** 20 byte message handle */
    private byte[] messageHandle;

    /**
     * Constructor
     * 
     * @param endpointIndex 2 byte endpoint index of the artifact
     * @param sourceID 20 byte source ID of the artifact
     * @param messageHandle 20 byte message handle of the artifact
     * 
     * @throws IllegalArgumentException thrown if the endpoint index, source ID, or message handle arrays are not of the
     *             right size
     */
    public SAML2ArtifactType0004(byte[] endpointIndex, byte[] sourceID, byte[] messageHandle)
            throws IllegalArgumentException {
        super(TYPE_CODE, endpointIndex);
        setSourceID(sourceID);
        setMessageHandle(messageHandle);
    }

    /**
     * Constructs a SAML 2 artifact from it's byte array representation.
     * 
     * @param artifact the byte array representing the artifact
     * 
     * @throws IllegalArgumentException thrown if the artifact is not the right type or lenght (44 bytes)
     */
    public SAML2ArtifactType0004(byte[] artifact) throws IllegalArgumentException {
        if (artifact.length != 44) {
            throw new IllegalArgumentException("Artifact length must be 44 bytes it was " + artifact.length + "bytes");
        }

        byte[] typeCode = { artifact[0], artifact[1] };
        if (Arrays.equals(typeCode, TYPE_CODE)) {
            throw new IllegalArgumentException("Illegal artifact type code");
        }
        setTypeCode(typeCode);

        byte[] endpointIndex = { artifact[2], artifact[3] };
        setEndpointIndex(endpointIndex);

        byte[] sourceID = new byte[20];
        System.arraycopy(artifact, 4, sourceID, 0, 20);
        setSourceID(sourceID);

        byte[] messageHandle = new byte[20];
        System.arraycopy(artifact, 24, messageHandle, 0, 20);
        setMessageHandle(messageHandle);
    }

    /**
     * Gets the 20 byte source ID of the artifact.
     * 
     * @return the source ID of the artifact
     */
    public byte[] getSourceID() {
        return sourceID;
    }

    /**
     * Sets the 20 byte source ID of the artifact.
     * 
     * @param newSourceID 20 byte source ID of the artifact
     * 
     * @throws IllegalArgumentException thrown if the given source ID is not 20 bytes
     */
    protected void setSourceID(byte[] newSourceID) throws IllegalArgumentException {
        if (newSourceID.length != 20) {
            throw new IllegalArgumentException("Artifact source ID must be 20 bytes long");
        }
        sourceID = newSourceID;
    }

    /**
     * Gets the 20 byte message handle of the artifact.
     * 
     * @return 20 byte message handle of the artifact
     */
    public byte[] getMessageHandle() {
        return messageHandle;
    }

    /**
     * Sets the 20 byte message handle of the artifact.
     * 
     * @param messageHandle 20 byte message handle of the artifact
     */
    public void setMessageHandle(byte[] messageHandle) {
        if (messageHandle.length != 20) {
            throw new IllegalArgumentException("Artifact message handle must be 20 bytes long");
        }
        this.messageHandle = messageHandle;
    }

    /** {@inheritDoc} */
    public byte[] getRemainingArtifact() {
        byte[] remainingArtifact = new byte[20];

        System.arraycopy(getSourceID(), 0, remainingArtifact, 0, 20);
        System.arraycopy(getMessageHandle(), 0, remainingArtifact, 0, 20);

        return remainingArtifact;
    }
}