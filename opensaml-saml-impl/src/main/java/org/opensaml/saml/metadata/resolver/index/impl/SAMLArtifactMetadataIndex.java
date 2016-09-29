/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.saml.metadata.resolver.index.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.binding.artifact.SAMLArtifact;
import org.opensaml.saml.common.binding.artifact.SAMLSourceIDArtifact;
import org.opensaml.saml.common.binding.artifact.SAMLSourceLocationArtifact;
import org.opensaml.saml.criterion.ArtifactCriterion;
import org.opensaml.saml.ext.saml1md.SourceID;
import org.opensaml.saml.metadata.resolver.index.MetadataIndex;
import org.opensaml.saml.metadata.resolver.index.MetadataIndexKey;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.security.crypto.JCAConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;

/**
 * An implementation of {@link MetadataIndex} which indexes entities by their artifact SourceID values.
 */
public class SAMLArtifactMetadataIndex implements MetadataIndex {
    
    /** Indexing function instance to use. */
    private List<Function<EntityDescriptor, Set<MetadataIndexKey>>> indexingFunctions;
    
    /**
     * Constructor.
     * 
     * <p>
     * The descriptor indexing functions will be:
     * <ul>
     *   <li>{@link EntityIDToSHA1SourceIDIndexingFunction}</li>
     *   <li>{@link SourceIDExtensionIndexingFunction}</li>
     * </ul>
     * </p>
     */
    public SAMLArtifactMetadataIndex() {
        this(Lists.<Function<EntityDescriptor, Set<MetadataIndexKey>>>newArrayList(
                new EntityIDToSHA1SourceIDIndexingFunction(),
                new SourceIDExtensionIndexingFunction()
                //TODO add source location indexing function when implemented
                ));
    }
    
    /**
     * Constructor.
     *
     * @param descriptorIndexingFunctions the functions used to produce index keys from an entity descriptor
     */
    public SAMLArtifactMetadataIndex(
            @Nonnull final List<Function<EntityDescriptor, Set<MetadataIndexKey>>> descriptorIndexingFunctions) {
        indexingFunctions = Constraint.isNotNull(descriptorIndexingFunctions, 
                "EntityDescriptor indexing functions list may not be null");
    }

    /** {@inheritDoc} */
    @Nullable public Set<MetadataIndexKey> generateKeys(@Nonnull EntityDescriptor descriptor) {
        Constraint.isNotNull(descriptor, "EntityDescriptor was null");
        HashSet<MetadataIndexKey> results = new HashSet<>();
        for (Function<EntityDescriptor, Set<MetadataIndexKey>> indexingFunction : indexingFunctions) {
            Set<MetadataIndexKey> result = indexingFunction.apply(descriptor);
            if (result != null) {
                results.addAll(result);
            }
        }
        return results;
    }

    /** {@inheritDoc} */
    @Nullable public Set<MetadataIndexKey> generateKeys(@Nonnull CriteriaSet criteriaSet) {
        Constraint.isNotNull(criteriaSet, "CriteriaSet was null");
        ArtifactCriterion artifactCrit = criteriaSet.get(ArtifactCriterion.class);
        if (artifactCrit != null) {
            LazySet<MetadataIndexKey> results = new LazySet<>();
            
            SAMLArtifact artifact = artifactCrit.getArtifact();
            
            if (artifact instanceof SAMLSourceIDArtifact) {
                results.add(new ArtifactSourceIDMetadataIndexKey(((SAMLSourceIDArtifact)artifact).getSourceID()));
            }
            
            if (artifact instanceof SAMLSourceLocationArtifact) {
                //TODO can reuse Endpoint index key from EndpointMetadataIndex, or need new key type?
            }
            
            return results;
        } else {
            return null;
        }
    }
    
    /**
     * Entity descriptor indexing function which produces a single 
     * {@link ArtifactSourceIDMetadataIndexKey} based on the SHA-1 digest of the UTF-8 encoding
     * of the value of {@link EntityDescriptor#getEntityID()}.
     */
    public static class EntityIDToSHA1SourceIDIndexingFunction 
        implements Function<EntityDescriptor, Set<MetadataIndexKey>> {
        
        /** Logger. */
        private Logger log = LoggerFactory.getLogger(EntityIDToSHA1SourceIDIndexingFunction.class);

        /** {@inheritDoc} */
        public Set<MetadataIndexKey> apply(@Nonnull final EntityDescriptor descriptor) {
            if (descriptor == null) {
                return null;
            }
            String entityID = StringSupport.trimOrNull(descriptor.getEntityID());
            if (entityID == null) {
                return null;
            }
            
            try {
                MessageDigest sha1Digester = MessageDigest.getInstance(JCAConstants.DIGEST_SHA1);
                byte[] sourceID = sha1Digester.digest(entityID.getBytes("UTF-8"));
                if (log.isTraceEnabled()) {
                    log.trace("For entityID '{}' produced artifact SourceID index value '{}'", 
                            entityID, Hex.encodeHex(sourceID, true));
                }
                return Collections.<MetadataIndexKey>singleton(new ArtifactSourceIDMetadataIndexKey(sourceID));
            } catch (NoSuchAlgorithmException e) {
                // SHA-1 should be supported in every JVM, so this should never happen.
                log.error("Digest algorithm '{}' was invalid for encoding artifact SourceID", 
                        JCAConstants.DIGEST_SHA1, e);
                return null;
            } catch (UnsupportedEncodingException e) {
                // UTF-8 should be supported in every JVM, this should never happen.
                log.error("UTF-8 was unsupported for encoding artifact SourceID!");
                return null;
            }
        }
        
    }
    
    /**
     * Descriptor indexing function which produces 0 to many {@link ArtifactSourceIDMetadataIndexKey} instances
     * based on the values of all {@link SourceID} extension elements present in the descriptor's
     * {@link RoleDescriptor}s.
     */
    public static class SourceIDExtensionIndexingFunction 
        implements Function<EntityDescriptor, Set<MetadataIndexKey>> {
        
        /** Logger. */
        private Logger log = LoggerFactory.getLogger(SourceIDExtensionIndexingFunction.class);

        /** {@inheritDoc} */
        public Set<MetadataIndexKey> apply(@Nonnull final EntityDescriptor descriptor) {
            if (descriptor == null) {
                return null;
            }
            
            LazySet<MetadataIndexKey> results = new LazySet<>();
            
            for (RoleDescriptor roleDescriptor : descriptor.getRoleDescriptors()) {
                Extensions extensions = roleDescriptor.getExtensions();
                if (extensions != null) {
                    List<XMLObject> children = extensions.getUnknownXMLObjects(SourceID.DEFAULT_ELEMENT_NAME);
                    if (children != null) {
                        QName role = descriptor.getSchemaType() != null ? descriptor.getSchemaType() 
                                : descriptor.getElementQName();
                        log.trace("Processing SourceID extensions for entityID '{}' with role '{}'", 
                                descriptor.getEntityID(), role);
                        
                        for (XMLObject child : children) {
                            SourceID extSourceID = (SourceID) child;
                            String extSourceIDHex = StringSupport.trimOrNull(extSourceID.getValue());
                            if (extSourceIDHex != null) {
                                try {
                                    byte[] sourceID = Hex.decodeHex(extSourceIDHex.toCharArray());
                                    if (log.isTraceEnabled()) {
                                        log.trace("For SourceID extension value '{}' produced indexed value '{}'", 
                                                extSourceIDHex, Hex.encodeHex(sourceID, true));
                                    }
                                    results.add(new ArtifactSourceIDMetadataIndexKey(sourceID));
                                } catch (DecoderException e) {
                                    log.warn("Error decoding hexidecimal SourceID extension value '{}' for indexing", 
                                            extSourceIDHex, e);
                                }
                            }
                        }
                    }
                }
            }
            
            
            return results;
        }
        
    }
    
    /**
     * An implementation of {@link MetadataIndexKey} representing a SAML artifact SourceID value.
     */
    protected static class ArtifactSourceIDMetadataIndexKey implements MetadataIndexKey {
        
        /** The SourceID value. */
        @Nonnull @NotEmpty private final byte[] sourceID;

        /**
         * Constructor.
         * 
         * @param newSourceID the artifact SourceID value
         */
        public ArtifactSourceIDMetadataIndexKey(@Nonnull @NotEmpty final byte[] newSourceID) {
            sourceID = Constraint.isNotNull(newSourceID, "SourceID cannot be null");
            Constraint.isGreaterThan(0, sourceID.length, "SourceID length must be greater than zero");
        }

        /**
         * Get the SourceID value.
         * 
         * @return the SourceID value
         */
        @Nonnull @NotEmpty public byte[] getSourceID() {
            return sourceID;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("sourceID", Hex.encodeHex(sourceID, true))
                    .toString();
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return Arrays.hashCode(sourceID);
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof ArtifactSourceIDMetadataIndexKey) {
                return Arrays.equals(sourceID, ((ArtifactSourceIDMetadataIndexKey) obj).getSourceID());
            }

            return false;
        }
        
    }

}
