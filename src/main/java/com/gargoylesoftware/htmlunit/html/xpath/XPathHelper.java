/*
 * Copyright (c) 2002-2022 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.html.xpath;

import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XNodeSet;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gargoylesoftware.htmlunit.html.DomNode;

/**
 * Collection of XPath utility methods.
 *
 * @author Ahmed Ashour
 * @author Chuck Dumont
 */
public final class XPathHelper {

    private static final ThreadLocal<Boolean> PROCESS_XPATH_ = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Private to avoid instantiation.
     */
    private XPathHelper() {
        // Empty.
    }

    /**
     * Evaluates an XPath expression from the specified node, returning the resultant nodes.
     *
     * @param <T> the type class
     * @param node the node to start searching from
     * @param xpathExpr the XPath expression
     * @param resolver the prefix resolver to use for resolving namespace prefixes, or null
     * @return the list of objects found
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getByXPath(final DomNode node, final String xpathExpr,
            final PrefixResolver resolver) {
        if (xpathExpr == null) {
            throw new IllegalArgumentException("Null is not a valid XPath expression");
        }

        PROCESS_XPATH_.set(Boolean.TRUE);
        final List<T> list = new ArrayList<>();
        try {
            final XObject result = evaluateXPath(node, xpathExpr, resolver);

            if (result instanceof XNodeSet) {
                final NodeList nodelist = result.nodelist();
                for (int i = 0; i < nodelist.getLength(); i++) {
                    list.add((T) nodelist.item(i));
                }
            }
            else if (result instanceof XNumber) {
                list.add((T) Double.valueOf(result.num()));
            }
            else if (result instanceof XBoolean) {
                list.add((T) Boolean.valueOf(result.bool()));
            }
            else if (result instanceof XString) {
                list.add((T) result.str());
            }
            else {
                throw new RuntimeException("Unproccessed " + result.getClass().getName());
            }
        }
        catch (final Exception e) {
            throw new RuntimeException("Could not retrieve XPath >" + xpathExpr + "< on " + node, e);
        }
        finally {
            PROCESS_XPATH_.set(Boolean.FALSE);
        }
        return list;
    }

    /**
     * Returns whether the thread is currently evaluating XPath expression or no.
     * @return whether the thread is currently evaluating XPath expression or no
     */
    public static boolean isProcessingXPath() {
        return PROCESS_XPATH_.get().booleanValue();
    }

    /**
     * Evaluates an XPath expression to an XObject.
     * @param contextNode the node to start searching from
     * @param str a valid XPath string
     * @param prefixResolver prefix resolver to use for resolving namespace prefixes, or null
     * @return an XObject, which can be used to obtain a string, number, nodelist, etc (should never be {@code null})
     * @throws TransformerException if a syntax or other error occurs
     */
    private static XObject evaluateXPath(final DomNode contextNode,
            final String str, final PrefixResolver prefixResolver) throws TransformerException {
        final XPathContext xpathSupport = new XPathContext();
        final Node xpathExpressionContext;
        if (contextNode.getNodeType() == Node.DOCUMENT_NODE) {
            xpathExpressionContext = ((Document) contextNode).getDocumentElement();
        }
        else {
            xpathExpressionContext = contextNode;
        }

        PrefixResolver resolver = prefixResolver;
        if (resolver == null) {
            resolver = new HtmlUnitPrefixResolver(xpathExpressionContext);
        }

        final boolean caseSensitive = contextNode.getPage().hasCaseSensitiveTagNames();

        final XPathAdapter xpath = new XPathAdapter(str, null, resolver, null, caseSensitive);
        final int ctxtNode = xpathSupport.getDTMHandleFromNode(contextNode);
        return xpath.execute(xpathSupport, ctxtNode, prefixResolver);
    }

}
