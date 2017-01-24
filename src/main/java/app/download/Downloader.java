/*
* @author Alberto Vilches
* @date 22/12/2016
*/
package app.download;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class Downloader {
    protected static final String mimeSeparation = "FILESRV_MIME_BOUNDARY";
    protected static final int INPUT_BUFFER_SIZE = 4096;
    protected static final int RESPONSE_BUFFER_SIZE = 2048;

//    https://svn.apache.org/repos/asf/tomcat/tc8.5.x/trunk/java/org/apache/catalina/servlets/DefaultServlet.java

    public void serve(HttpServletRequest request,
                      HttpServletResponse response,
                      File file, String filename,
                      int cps, boolean allowRanges, DownloadHandler downloadHandler)
            throws IOException, ServletException {

        long contentLength = file.length();
        boolean serveContent = contentLength > 0 && !request.getMethod().equalsIgnoreCase("head");
        String rangeHeader = null;
        if (allowRanges) {
            rangeHeader = request.getHeader("Range");
            response.setHeader("Accept-Ranges", "bytes");
        }
        OutputStream ostream = serveContent && cps > 0 ? new SlowOutputStream(response.getOutputStream(), cps) : response.getOutputStream();

        if (rangeHeader == null) {
            // System.out.println(request.getMethod());
            // Set the appropriate output headers
            configureFileNameHeader(response, filename);
            response.setContentType("application/octet-stream");
            response.setContentLength((int) contentLength);

            if (serveContent) {
                configureBuffer(response);
                response.setStatus(HttpServletResponse.SC_OK);
                dumpAll(new FileInputStream(file), ostream, downloadHandler);
            }

        } else {
            List<Range> ranges = parseRange(rangeHeader, response, contentLength);

            if (ranges == null || ranges.isEmpty()) {
                response.addHeader("Content-Range", "bytes */" + contentLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            if (ranges.size() == 1) {
//                System.out.println(request.getMethod()+ " "+ranges.get(0));
                Range range = ranges.get(0);
                response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
                long length = range.end - range.start + 1;
                response.setContentLength((int) length);
                configureFileNameHeader(response, filename);
                response.setContentType("application/octet-stream");
                if (serveContent) {
                    configureBuffer(response);
                    dumpOneRange(new FileInputStream(file), ostream, range, downloadHandler);
                }
            } else {
//                System.out.println(request.getMethod()+ " "+ranges.get(0)+" ...");
                configureFileNameHeader(response, filename);
                response.setContentType("multipart/byteranges; boundary=" + mimeSeparation);
                if (serveContent) {
                    configureBuffer(response);
                    dumpMultipleRanges(new FileInputStream(file), ostream, ranges.iterator(), downloadHandler);
                }
            }
        }
    }

    private void configureFileNameHeader(HttpServletResponse response, String filename) {
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
    }

    private void configureBuffer(HttpServletResponse response) {
        try {
            response.setBufferSize(RESPONSE_BUFFER_SIZE);
        } catch (IllegalStateException e) {
            // Silent catch
        }
    }

    protected void dumpAll(InputStream is, OutputStream ostream, DownloadHandler downloadHandler)
            throws IOException {

        BufferedInputStream istream = new BufferedInputStream(is, INPUT_BUFFER_SIZE);
        byte buffer[] = new byte[INPUT_BUFFER_SIZE];
        try {
            while (true) {
                int len = istream.read(buffer);
                if (len == -1) break;
                ostream.write(buffer, 0, len);
                if (!downloadHandler.download(len)) {
                    throw new IOException("Cancelado por el admin o state no es download");
                }
            }
        } catch (IOException e) {
            downloadHandler.abort();
        } finally {
            try {
                istream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param ostream The output stream to write to
     * @param range   Range the client wanted to retrieve
     * @throws IOException if an input/output error occurs
     */
    protected void dumpOneRange(InputStream resourceInputStream, OutputStream ostream, Range range, DownloadHandler downloadHandler)
            throws IOException {

        InputStream istream = new BufferedInputStream(resourceInputStream, INPUT_BUFFER_SIZE);
        IOException exception = copyRange(istream, ostream, range.start, range.end, downloadHandler);

        // Clean up the input stream
        istream.close();

        // Rethrow any exception that has occurred
        if (exception != null) {
            downloadHandler.abort();
        }
    }

    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param ostream The output stream to write to
     * @param ranges  Enumeration of the ranges the client wanted to
     *                retrieve
     * @throws IOException if an input/output error occurs
     */
    protected void dumpMultipleRanges(InputStream resourceInputStream, OutputStream ostream, Iterator<Range> ranges, DownloadHandler downloadHandler)
            throws IOException {

        IOException exception = null;

        while ((exception == null) && (ranges.hasNext())) {
            try (InputStream istream = new BufferedInputStream(resourceInputStream, INPUT_BUFFER_SIZE)) {
                Range currentRange = ranges.next();
                // Writing MIME header.
                String mimeHeader = "\r\n--" + mimeSeparation + "\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "Content-Range: bytes " + currentRange.start + "-" + currentRange.end + "/" + currentRange.length + "\r\n\r\n";
                ostream.write(mimeHeader.getBytes(Charset.forName("UTF8")));
                // Printing content
                exception = copyRange(istream, ostream, currentRange.start, currentRange.end, downloadHandler);
                istream.close();
            }
        }

        String mimeHeader = "\r\n--" + mimeSeparation + "--";
        ostream.write(mimeHeader.getBytes(Charset.forName("UTF8")));
        // Rethrow any exception that has occurred
        if (exception != null) {
            downloadHandler.abort();
        }
    }

    /**
     * Copy the contents of the specified input stream to the specified
     * output stream, and ensure that both streams are closed before returning
     * (even in the face of an exception).
     *
     * @param istream The input stream to read from
     * @param ostream The output stream to write to
     * @param start   Start of the range which will be copied
     * @param end     End of the range which will be copied
     * @return Exception which occurred during processing
     */
    protected IOException copyRange(InputStream istream, OutputStream ostream, long start, long end, DownloadHandler downloadHandler) {

//             log("Serving bytes:" + start + "-" + end);

        long skipped = 0;
        try {
            skipped = istream.skip(start);
        } catch (IOException e) {
            return e;
        }
        if (skipped < start) {
            return new IOException("Error skipping " +
                    Long.valueOf(skipped) + " fron " + Long.valueOf(start));
        }

        IOException exception = null;
        long bytesToRead = end - start + 1;

        byte buffer[] = new byte[INPUT_BUFFER_SIZE];
        int len = buffer.length;
        while ((bytesToRead > 0) && (len >= buffer.length)) {
            try {
                len = istream.read(buffer);
                if (bytesToRead >= len) {
                    ostream.write(buffer, 0, len);
                    if (!downloadHandler.download(len)) {
                        throw new IOException("Cancelado por el admin o state no es download");
                    }
                    bytesToRead -= len;
                } else {
                    ostream.write(buffer, 0, (int) bytesToRead);
                    if (!downloadHandler.download(bytesToRead)) {
                        throw new IOException("Cancelado por el admin o state no es download");
                    }
                    bytesToRead = 0;
                }
            } catch (IOException e) {
                exception = e;
                len = -1;
            }
            if (len < buffer.length)
                break;
        }
        return exception;
    }

    /**
     * Parse the range header.
     *
     * @param rangeHeader range header
     * @param response    The servlet response we are creating
     * @return Vector of ranges
     */
    protected List<Range> parseRange(String rangeHeader, HttpServletResponse response,
                                     long contentLength)
            throws IOException {

        if (!rangeHeader.startsWith("bytes")) {
            return null;
        }

        rangeHeader = rangeHeader.substring(6);

        // Vector which will contain all the ranges which are successfully parsed.
        List<Range> result = new ArrayList<>();
        StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

        // Parsing the range list
        while (commaTokenizer.hasMoreTokens()) {
            String rangeDefinition = commaTokenizer.nextToken().trim();

            Range currentRange = new Range();
            currentRange.length = contentLength;

            int dashPos = rangeDefinition.indexOf('-');

            if (dashPos == -1) {
                return null;
            }

            if (dashPos == 0) {
                try {
                    long offset = Long.parseLong(rangeDefinition);
                    currentRange.start = contentLength + offset;
                    currentRange.end = contentLength - 1;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                try {
                    currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1)
                        currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1, rangeDefinition.length()));
                    else
                        currentRange.end = contentLength - 1;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (!currentRange.validate()) {
                return null;
            }
            result.add(currentRange);
        }
        return result;
    }

    protected class Range {

        public long start;
        public long end;
        public long length;

        @Override
        public String toString() {
            return "Range{" + "start=" + start + ", end=" + end + ", length=" + length + '}';
        }

        /**
         * Validate range.
         */
        public boolean validate() {
            if (end >= length) {
                end = length - 1; // Fix
            }
            return start >= 0 && end >= 0 && start <= end && length > 0;
        }

    }
}

