/*
* @author Alberto Vilches
* @date 24/01/2017
*/
package tools;

/*
* @author Alberto Vilches
* @date 06/01/2017
*/

public class StringTools {

    public static String humanReadableBytes(long bytes) {
        return humanReadableBytes(bytes, true);
    }

    public static String humanReadableBytes(long bytes, boolean si) {
        String minus = bytes < 0 ? "-" : "";
        bytes = Math.abs(bytes);
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return "" + bytes;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return minus + String.format("%.1f", bytes / Math.pow(unit, exp));
//        return String.format("%.1f", bytes / Math.pow(unit, exp));
    }

    public static String humanReadableString(long bytes) {
        return humanReadableString(bytes, true);
    }

    public static String humanReadableString(long bytes, boolean si) {
        if (bytes == 0) return "0 MB";
        String minus = bytes < 0 ? "-" : "";
        bytes = Math.abs(bytes);
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " bytes";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return minus + String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
//        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String humanReadableSize(long bytes) {
        return humanReadableSize(bytes, true);
    }

    public static String humanReadableSize(long bytes, boolean si) {
        if (bytes == 0) return "MB";
        bytes = Math.abs(bytes);
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return "bytes";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return pre + "B";
    }
}
