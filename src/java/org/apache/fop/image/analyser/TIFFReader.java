/*
 * $Id: TIFFReader.java,v 1.9 2003/03/06 21:25:45 jeremias Exp $
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 * 
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 * 
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 * 
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */ 
package org.apache.fop.image.analyser;

// Java
import java.io.InputStream;
import java.io.IOException;

// FOP
import org.apache.fop.image.FopImage;
import org.apache.fop.fo.FOUserAgent;

/**
 * ImageReader object for TIFF image type.
 *
 * @author    Pankaj Narula, Michael Lee
 * @version   $Id: TIFFReader.java,v 1.9 2003/03/06 21:25:45 jeremias Exp $
 */
public class TIFFReader implements ImageReader {

    private static final int TIFF_SIG_LENGTH = 8;

    /** @see org.apache.fop.image.analyser.ImageReader */
    public FopImage.ImageInfo verifySignature(String uri, InputStream bis,
                FOUserAgent ua) throws IOException {
        byte[] header = getDefaultHeader(bis);
        boolean supported = false;

        // first 2 bytes = II (little endian encoding)
        if (header[0] == (byte) 0x49 && header[1] == (byte) 0x49) {

            // look for '42' in byte 3 and '0' in byte 4
            if (header[2] == 42 && header[3] == 0) {
                supported = true;
            }
        }

        // first 2 bytes == MM (big endian encoding)
        if (header[0] == (byte) 0x4D && header[1] == (byte) 0x4D) {

            // look for '42' in byte 4 and '0' in byte 3
            if (header[2] == 0 && header[3] == 42) {
                supported = true;
            }
        }

        if (supported) {
            FopImage.ImageInfo info = getDimension(header);
            info.mimeType = getMimeType();
            info.inputStream = bis;
            return info;
        } else {
            return null;
        }
    }

    /**
     * Returns the MIME type supported by this implementation.
     *
     * @return   The MIME type
     */
    public String getMimeType() {
        return "image/tiff";
    }

    private FopImage.ImageInfo getDimension(byte[] header) {
        // currently not setting the width and height
        // these are set again by the Jimi image reader.
        // I suppose I'll do it one day to be complete.  Or
        // someone else will.
        // Note: bytes 4,5,6,7 contain the byte offset in the stream of the first IFD block
        /*
         * //png is always big endian
         * int byte1 = header[ 16 ] & 0xff;
         * int byte2 = header[ 17 ] & 0xff;
         * int byte3 = header[ 18 ] & 0xff;
         * int byte4 = header[ 19 ] & 0xff;
         * long l = ( long ) ( ( byte1 << 24 ) | ( byte2 << 16 ) |
         * ( byte3 << 8 ) | byte4 );
         * this.width = ( int ) ( l );
         * byte1 = header[ 20 ] & 0xff;
         * byte2 = header[ 21 ] & 0xff;
         * byte3 = header[ 22 ] & 0xff;
         * byte4 = header[ 23 ] & 0xff;
         * l = ( long ) ( ( byte1 << 24 ) | ( byte2 << 16 ) | ( byte3 << 8 ) |
         * byte4 );
         * this.height = ( int ) ( l );
         */
        FopImage.ImageInfo info = new FopImage.ImageInfo();
        info.width = -1;
        info.height = -1;
        return info;
    }

    private byte[] getDefaultHeader(InputStream imageStream)
        throws IOException {
        byte[] header = new byte[TIFF_SIG_LENGTH];
        try {
            imageStream.mark(TIFF_SIG_LENGTH + 1);
            imageStream.read(header);
            imageStream.reset();
        } catch (IOException ex) {
            try {
                imageStream.reset();
            } catch (IOException exbis) {
                // throw the original exception, not this one
            }
            throw ex;
        }
        return header;
    }

}

