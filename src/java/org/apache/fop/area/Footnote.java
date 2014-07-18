/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.area;

// may combine with before float into a conditional area

/**
 * The footnote-reference-area optionally generated by an fo:region-body.
 * This areas holds footnote areas and an optional separator area.
 * See fo:region-body definition in the XSL Rec for more information.
 */
public class Footnote extends BlockParent {

    private static final long serialVersionUID = -7907428219886367161L;

    private Block separator;

    // footnote has an optional separator
    // and a list of sub block areas that can be added/removed

    // this is the relative position of the footnote inside
    // the body region
    private int top;

    /**
     * Set the separator area for this footnote.
     *
     * @param sep the separator area
     */
    public void setSeparator(Block sep) {
        separator = sep;
    }

    /**
     * Get the separator area for this footnote area.
     *
     * @return the separator area
     */
    public Block getSeparator() {
        return separator;
    }

    /**
     * Set the relative position of the footnote inside the body region.
     *
     * @param top the relative position.
     */
    public void setTop(int top) {
        this.top = top;
    }

    /**
     * Get the relative position of the footnote inside the body region.
     *
     * @return the relative position.
     */
    public int getTop() {
        return top;
    }

    /**
     * Add a block area as child to the footnote area
     *
     * @param child the block area.
     */
    @Override
    public void addBlock(Block child) {
        addChildArea(child);
        setBPD(getBPD() + child.getAllocBPD());
    }

}

