/*
* @author Alberto Vilches
* @date 22/12/2016
*/
package app;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

public class FileDownload {
    protected int INPUT_BUFFER_SIZE = 2048;
    protected int RESPONSE_BUFFER_SIZE = 2048;

    protected void serveResource(HttpServletRequest request, HttpServletResponse response,
                                 File file) throws IOException {

        String contentType = "application/octet-stream";
        long fileLength = file.length();
        List<Range> ranges = parseRange(request, response, fileLength);
        if (ranges == null || ranges.get(0).end == (fileLength - 1)) {
//            finished
        }
        response.setHeader("Content-disposition", "attachment;filename="+file.getName());

        long contentLength = file.length();
        response.setHeader("Accept-Ranges", "bytes");

        // Parse range specifier
        ServletOutputStream ostream = response.getOutputStream();

        if (ranges == null || ranges.isEmpty()) {

            response.setContentType(contentType);

            if (contentLength >= 0) {
                if (contentLength < Integer.MAX_VALUE) {
                    response.setContentLength((int) contentLength);
                } else {
                    response.setHeader("content-length", String.valueOf(contentLength));
                }
            }

            try {
                response.setBufferSize(RESPONSE_BUFFER_SIZE);
            } catch (IllegalStateException e) {
                // Silent catch
            }

            dump(file, ostream);

        } else {

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            if (ranges.size() == 1) {

                Range range = (Range) ranges.get(0);
                response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);

                long length = range.end - range.start + 1;
                if (length < Integer.MAX_VALUE) {
                    response.setContentLength((int) length);
                } else {
                    response.setHeader("content-length", String.valueOf(contentLength));
                }

                response.setContentType(contentType);

                try {
                    response.setBufferSize(RESPONSE_BUFFER_SIZE);
                } catch (IllegalStateException e) {
                    // Silent catch
                }
                dump(file, ostream, range);

            } else {

                response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);

            }

        }

    }


    /**
     * Parse the range header.
     *
     * @param request  The servlet request we are processing
     * @param response The servlet response we are creating
     * @return Vector of ranges
     */
    public List<Range> parseRange(HttpServletRequest request, HttpServletResponse response,
                                long fileLength) throws IOException {

        String rangeHeader = request.getHeader("Range");

        if (rangeHeader == null) return null;

        if (!rangeHeader.startsWith("bytes")) {
            response.addHeader("Content-Range", "bytes */" + fileLength);
            response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            throw new IOException("Invalid range");
        }

        rangeHeader = rangeHeader.substring(6);

        // Vector which will contain all the ranges which are successfully parsed.
        List<Range> result = new ArrayList<Range>();
        StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

        // Parsing the range list
        while (commaTokenizer.hasMoreTokens()) {
            String rangeDefinition = commaTokenizer.nextToken().trim();

            Range currentRange = new Range();
            currentRange.length = fileLength;

            int dashPos = rangeDefinition.indexOf('-');

            if (dashPos == -1) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                throw new IOException("Invalid range");
            }

            if (dashPos == 0) {

                try {
                    long offset = Long.parseLong(rangeDefinition);
                    currentRange.start = fileLength + offset;
                    currentRange.end = fileLength - 1;
                } catch (NumberFormatException e) {
                    response.addHeader("Content-Range", "bytes */" + fileLength);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    throw new IOException("Invalid range");
                }

            } else {

                try {
                    currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
                    if (dashPos < rangeDefinition.length() - 1) {
                        currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1, rangeDefinition.length()));
                    } else {
                        currentRange.end = fileLength - 1;
                    }
                } catch (NumberFormatException e) {
                    response.addHeader("Content-Range", "bytes */" + fileLength);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    throw new IOException("Invalid range");
                }
            }

            if (!currentRange.validate()) {
                response.addHeader("Content-Range", "bytes */" + fileLength);
                response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                throw new IOException("Invalid range");
            }

            result.add(currentRange);
        }

        return result;
    }

    protected void dump(File file, ServletOutputStream ostream) throws IOException {

        InputStream istream = new BufferedInputStream(new FileInputStream(file), INPUT_BUFFER_SIZE);
        IOException exception = null;
        byte buffer[] = new byte[INPUT_BUFFER_SIZE];
        int len = buffer.length;
        while (true) {
            try {
                len = istream.read(buffer);
                if (len == -1) break;
                ostream.write(buffer, 0, len);
            } catch (IOException e) {
                exception = e;
                len = -1;
                break;
            }
        }
        istream.close();
        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }


    protected void dump(File file, ServletOutputStream ostream, Range range)
            throws IOException {

        InputStream istream = new BufferedInputStream(new FileInputStream(file), INPUT_BUFFER_SIZE);
        istream.skip(range.start);

        IOException exception = null;
        long bytesToRead = range.end - range.start + 1;
        byte buffer[] = new byte[INPUT_BUFFER_SIZE];
        int len = buffer.length;
        while (bytesToRead > 0 && len >= buffer.length) {
            try {
                len = istream.read(buffer);
                if (bytesToRead >= len) {
                    ostream.write(buffer, 0, len);
                    bytesToRead -= len;
                } else {
                    ostream.write(buffer, 0, (int) bytesToRead);
                    bytesToRead = 0;
                }
            } catch (IOException e) {
                exception = e;
                len = -1;
            }
            if (len < buffer.length)
                break;
        }
        istream.close();
        // Rethrow any exception that has occurred
        if (exception != null)
            throw exception;

    }

    protected class Range {

        public long start;
        public long end;
        public long length;

        /**
         * Validate range.
         */
        public boolean validate() {
            if (end >= length)
                end = length - 1;
            return ((start >= 0) && (end >= 0) && (start <= end)
                    && (length > 0));
        }

        public void recycle() {
            start = 0;
            end = 0;
            length = 0;
        }
    }
}

