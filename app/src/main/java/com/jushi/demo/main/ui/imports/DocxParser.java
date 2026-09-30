package com.jushi.demo.main.ui.imports;

import com.jushi.demo.main.data.entity.Course;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class DocxParser {

    private static final String NS_W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String NS_PKG = "http://schemas.microsoft.com/office/2006/xmlPackage";

    private static final Map<String, Integer> DAY_NAME_MAP = new HashMap<>();
    static {
        DAY_NAME_MAP.put("星期日", 1);
        DAY_NAME_MAP.put("星期天", 1);
        DAY_NAME_MAP.put("周日", 1);
        DAY_NAME_MAP.put("星期一", 2);
        DAY_NAME_MAP.put("周一", 2);
        DAY_NAME_MAP.put("星期二", 3);
        DAY_NAME_MAP.put("周二", 3);
        DAY_NAME_MAP.put("星期三", 4);
        DAY_NAME_MAP.put("周三", 4);
        DAY_NAME_MAP.put("星期四", 5);
        DAY_NAME_MAP.put("周四", 5);
        DAY_NAME_MAP.put("星期五", 6);
        DAY_NAME_MAP.put("周五", 6);
        DAY_NAME_MAP.put("星期六", 7);
        DAY_NAME_MAP.put("周六", 7);
    }

    private static final int[] PALETTE = {
            0xFF42A5F5, // blue
            0xFFEF5350, // red
            0xFF66BB6A, // green
            0xFFFFA726, // orange
            0xFFAB47BC, // purple
            0xFF26C6DA, // teal
            0xFFEC407A, // pink
            0xFF8D6E63, // brown
    };

    public static class ParseResult {
        public List<Course> courses;
        public String error;
        public String title;
        public int academicYear;
        public int semester; // 1=first, 2=second, 0=unknown

        public boolean isSuccess() { return error == null && courses != null; }
        public boolean hasSemesterInfo() { return academicYear > 0 && semester > 0; }
    }

    public static ParseResult parse(byte[] fileBytes) {
        ParseResult result = new ParseResult();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            Element docRoot;
            if (isFlatOpc(fileBytes)) {
                docRoot = extractDocumentElementFromFlatOpc(fileBytes, builder);
            } else if (isZipDocx(fileBytes)) {
                docRoot = extractDocumentElementFromZip(fileBytes, builder);
            } else {
                result.error = "文件格式错误：不是有效的DOCX文件";
                return result;
            }

            if (docRoot == null) {
                result.error = "无法提取文档内容";
                return result;
            }

            result.title = extractTitleText(docRoot);
            parseTitleSemester(result);

            NodeList tblList = docRoot.getElementsByTagNameNS(NS_W, "tbl");
            if (tblList == null || tblList.getLength() == 0) {
                result.error = "未找到课表数据";
                return result;
            }

            Element targetTable = null;
            for (int i = 0; i < tblList.getLength(); i++) {
                Element tbl = (Element) tblList.item(i);
                if (isTimetableTable(tbl)) {
                    targetTable = tbl;
                    break;
                }
            }

            if (targetTable == null) {
                result.error = "未找到课程表数据（缺少星期表头）";
                return result;
            }

            NodeList rows = targetTable.getElementsByTagNameNS(NS_W, "tr");
            if (rows == null || rows.getLength() < 2) {
                result.error = "课表中没有课程数据";
                return result;
            }

            Map<Integer, Integer> gridColToDay = buildGridColDayMap((Element) rows.item(0));
            if (gridColToDay.isEmpty()) {
                result.error = "无法解析课表表头";
                return result;
            }

            List<Course> courses = new ArrayList<>();
            Map<Integer, Course> vMergeMap = new HashMap<>();

            for (int i = 1; i < rows.getLength(); i++) {
                Element row = (Element) rows.item(i);
                int[] periods = parsePeriodFromRow(row);
                if (periods == null) continue;

                List<Element> cells = getChildElementsNS(row, NS_W, "tc");
                int gridCol = 0;
                for (int colIdx = 0; colIdx < cells.size(); colIdx++) {
                    Element cell = cells.get(colIdx);
                    int gs = getCellGridSpan(cell);
                    int cellGridCol = gridCol;
                    Integer day = gridColToDay.get(cellGridCol);
                    gridCol += gs;

                    if (day == null || day == 0) continue;

                    String vMerge = getVMergeVal(cell);
                    String cellText = extractCellText(cell);

                    if ("restart".equals(vMerge)) {
                        if (!cellText.isEmpty()) {
                            Course course = parseCourseFromCellText(cellText, day, periods[0], periods[0], courses.size());
                            if (course != null) {
                                vMergeMap.put(cellGridCol, course);
                                courses.add(course);
                            }
                        }
                    } else if (vMerge != null) {
                        // vMerge=continue: extend the course started in a previous row
                        Course pending = vMergeMap.get(cellGridCol);
                        if (pending != null) {
                            pending.setEndPeriod(periods[1]);
                        }
                    } else {
                        // No vMerge tag: merge chain ended at this column
                        vMergeMap.remove(cellGridCol);
                        if (!cellText.isEmpty()) {
                            Course course = parseCourseFromCellText(cellText, day, periods[0], periods[1], courses.size());
                            if (course != null) {
                                courses.add(course);
                            }
                        }
                    }
                }
            }

            result.courses = mergeConsecutiveCourses(courses);
            if (result.courses.isEmpty()) {
                result.error = "课表中没有识别到课程数据";
            }
        } catch (Exception e) {
            result.error = "无法解析课表文件: " + e.getMessage();
        }

        return result;
    }

    private static String extractTitleText(Element docRoot) {
        // Navigate to body, then collect text from paragraphs before the first table
        Element body = getFirstChildByLocalName(docRoot, "body");
        if (body == null) return "";

        NodeList bodyChildren = body.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bodyChildren.getLength(); i++) {
            Node child = bodyChildren.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "tbl".equals(child.getLocalName())) {
                break;
            }
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "p".equals(child.getLocalName())) {
                NodeList tNodes = ((Element) child).getElementsByTagNameNS(NS_W, "t");
                for (int j = 0; j < tNodes.getLength(); j++) {
                    String text = tNodes.item(j).getTextContent();
                    if (text != null) sb.append(text);
                }
            }
        }
        return sb.toString().trim();
    }

    private static void parseTitleSemester(ParseResult result) {
        if (result.title == null || result.title.isEmpty()) return;

        // Match "2025学年度第二学期" or "2025学年第二学期"
        Matcher m = Pattern.compile("(\\d{4})\\s*学年度?\\s*第\\s*([一二三])\\s*学期")
                .matcher(result.title);
        if (m.find()) {
            result.academicYear = Integer.parseInt(m.group(1));
            String semChar = m.group(2);
            if ("一".equals(semChar)) result.semester = 1;
            else if ("二".equals(semChar)) result.semester = 2;
            else if ("三".equals(semChar)) result.semester = 3;
        }
    }

    private static boolean isFlatOpc(byte[] bytes) {
        if (bytes.length < 5) return false;
        return bytes[0] == '<' && bytes[1] == '?' && bytes[2] == 'x' && bytes[3] == 'm' && bytes[4] == 'l';
    }

    private static boolean isZipDocx(byte[] bytes) {
        if (bytes.length < 4) return false;
        return bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 0x03 && bytes[3] == 0x04;
    }

    private static Element extractDocumentElementFromFlatOpc(byte[] fileBytes, DocumentBuilder builder) throws Exception {
        Document doc = builder.parse(new InputSource(new ByteArrayInputStream(fileBytes)));

        NodeList parts = doc.getElementsByTagNameNS(NS_PKG, "part");
        if (parts == null) return null;

        for (int i = 0; i < parts.getLength(); i++) {
            Element part = (Element) parts.item(i);
            String name = part.getAttributeNS(NS_PKG, "name");
            if ("/word/document.xml".equals(name)) {
                NodeList xmlDataList = part.getElementsByTagNameNS(NS_PKG, "xmlData");
                if (xmlDataList != null && xmlDataList.getLength() > 0) {
                    Element xmlData = (Element) xmlDataList.item(0);
                    NodeList docList = xmlData.getElementsByTagNameNS(NS_W, "document");
                    if (docList != null && docList.getLength() > 0) {
                        return (Element) docList.item(0);
                    }
                }
            }
        }
        return null;
    }

    private static Element extractDocumentElementFromZip(byte[] fileBytes, DocumentBuilder builder) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    zis.closeEntry();
                    Document doc = builder.parse(new InputSource(new ByteArrayInputStream(bos.toByteArray())));
                    return doc.getDocumentElement();
                }
                zis.closeEntry();
            }
        }
        return null;
    }

    private static boolean isTimetableTable(Element tbl) {
        NodeList rows = tbl.getElementsByTagNameNS(NS_W, "tr");
        if (rows == null || rows.getLength() == 0) return false;

        Element firstRow = (Element) rows.item(0);
        String rowText = collectRowText(firstRow);
        for (String key : DAY_NAME_MAP.keySet()) {
            if (rowText.contains(key)) return true;
        }
        if (rowText.contains("节次") || rowText.contains("课表")) return true;
        return false;
    }

    // Build mapping: gridCol index → dayOfWeek (using gridSpan from header)
    private static Map<Integer, Integer> buildGridColDayMap(Element headerRow) {
        Map<Integer, Integer> map = new HashMap<>();
        List<Element> cells = getChildElementsNS(headerRow, NS_W, "tc");
        int gridCol = 0;

        for (Element cell : cells) {
            int gs = getCellGridSpan(cell);
            String text = extractCellText(cell).trim();

            Integer day = null;
            for (Map.Entry<String, Integer> entry : DAY_NAME_MAP.entrySet()) {
                if (text.equals(entry.getKey()) || text.contains(entry.getKey())) {
                    day = entry.getValue();
                    break;
                }
            }

            // Map each grid column this cell spans to the day
            for (int j = 0; j < gs; j++) {
                map.put(gridCol + j, day != null ? day : 0);
            }
            gridCol += gs;
        }
        return map;
    }

    private static int getCellGridSpan(Element cell) {
        Element tcPr = getFirstChildByLocalName(cell, "tcPr");
        if (tcPr == null) return 1;

        Element gsEl = getFirstChildByLocalName(tcPr, "gridSpan");
        if (gsEl == null) return 1;

        String val = gsEl.getAttributeNS(NS_W, "val");
        if (val == null || val.isEmpty()) val = gsEl.getAttribute("w:val");
        if (val == null || val.isEmpty()) val = gsEl.getAttribute("val");
        if (val == null || val.isEmpty()) return 1;

        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return 1; }
    }

    private static int[] parsePeriodFromRow(Element row) {
        List<Element> cells = getChildElementsNS(row, NS_W, "tc");
        if (cells.isEmpty()) return null;
        String text = extractCellText(cells.get(0)).trim();
        if (text.isEmpty()) return null;

        // Match "第1-2节" (paired) or "第1节" (single)
        Matcher m = Pattern.compile("第\\s*(\\d+)\\s*-\\s*(\\d+)\\s*节").matcher(text);
        if (m.find()) {
            return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        }
        m = Pattern.compile("第\\s*(\\d+)\\s*节").matcher(text);
        if (m.find()) {
            int p = Integer.parseInt(m.group(1));
            return new int[]{p, p};
        }
        return null;
    }

    private static String getVMergeVal(Element cell) {
        // Iterate ALL tcPr children — some documents split attributes
        // across multiple tcPr elements (e.g. gridSpan in first, vMerge in second)
        NodeList children = cell.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "tcPr".equals(child.getLocalName())) {
                Element vMerge = getFirstChildByLocalName((Element) child, "vMerge");
                if (vMerge != null) {
                    return extractVMergeVal(vMerge);
                }
            }
        }

        // Fallback: vMerge as direct child of tc (non-standard but exists)
        Element vMerge = getFirstChildByLocalName(cell, "vMerge");
        if (vMerge != null) {
            return extractVMergeVal(vMerge);
        }

        return null;
    }

    private static String extractVMergeVal(Element vMerge) {
        String val = vMerge.getAttributeNS(NS_W, "val");
        if (val == null || val.isEmpty()) val = vMerge.getAttribute("w:val");
        // Fallback: iterate attributes directly (most robust)
//        if (val == null || val.isEmpty()) {
//            org.w3c.dom.NamedNodeMap attrs = vMerge.getAttributes();
//            for (int i = 0; i < attrs.getLength(); i++) {
//                org.w3c.dom.Node attr = attrs.item(i);
//                if ("val".equals(attr.getLocalName())) {
//                    val = attr.getNodeValue();
//                    break;
//                }
//            }
//        }
        if (val == null || val.isEmpty()) return "continue";
        return val;
    }

    // Find first direct child element by local name, regardless of namespace
    private static Element getFirstChildByLocalName(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && localName.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static String extractCellText(Element cell) {
        NodeList tNodes = cell.getElementsByTagNameNS(NS_W, "t");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tNodes.getLength(); i++) {
            String text = tNodes.item(i).getTextContent();
            if (text != null && !text.isEmpty()) {
                sb.append(text);
            }
        }
        return sb.toString();
    }

    private static Course parseCourseFromCellText(String cellText, int dayOfWeek,
                                                   int startPeriod, int endPeriod, int parseIndex) {
        if (cellText.trim().isEmpty()) return null;

        String[] parts = cellText.split("/");
        List<String> fields = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                fields.add(trimmed);
            }
        }
        if (fields.isEmpty()) return null;

        Course course = new Course();
        course.setDayOfWeek(dayOfWeek);
        course.setStartPeriod(startPeriod);
        course.setEndPeriod(endPeriod);

        // Field 1: week range (e.g., "1-17每周", "4-4每周")
        String weekRaw = fields.get(0);
        course.setWeekRange(extractWeekRange(weekRaw));

        // Field 2: course name (e.g., "本(专必)人工智能")
        if (fields.size() >= 2) {
            course.setCourseName(stripCourseTypePrefix(fields.get(1)));
        } else {
            course.setCourseName(fields.get(0));
        }

        // Field 3: teacher
        if (fields.size() >= 3) {
            course.setTeacher(fields.get(2));
        }

        // Field 4: location
        if (fields.size() >= 4) {
            course.setLocation(fields.get(3));
        }

        course.setColor(PALETTE[parseIndex % PALETTE.length]);
        return course;
    }

    private static String extractWeekRange(String text) {
        Matcher m = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)").matcher(text);
        if (m.find()) {
            return m.group(1) + "-" + m.group(2);
        }
        m = Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) {
            return m.group(1) + "-" + m.group(1);
        }
        return text;
    }

    private static String stripCourseTypePrefix(String text) {
        if (text == null) return null;
        // Remove prefix like "本(专必)", "本(公必)", "本(专选)" etc.
        return text.replaceFirst("^[本研]\\s*\\([^)]*\\)\\s*", "");
    }

    private static List<Course> mergeConsecutiveCourses(List<Course> raw) {
        if (raw == null || raw.size() <= 1) return raw;

        // Sort by day, then period
        raw.sort((a, b) -> {
            if (a.getDayOfWeek() != b.getDayOfWeek())
                return Integer.compare(a.getDayOfWeek(), b.getDayOfWeek());
            return Integer.compare(a.getStartPeriod(), b.getStartPeriod());
        });

        List<Course> merged = new ArrayList<>();
        Course current = raw.get(0);

        for (int i = 1; i < raw.size(); i++) {
            Course next = raw.get(i);
            if (current.getDayOfWeek() == next.getDayOfWeek()
                    && current.getEndPeriod() + 1 == next.getStartPeriod()
                    && eq(current.getCourseName(), next.getCourseName())
                    && eq(current.getWeekRange(), next.getWeekRange())) {
                // Merge: extend end period
                current.setEndPeriod(next.getEndPeriod());
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private static boolean eq(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private static List<Element> getChildElementsNS(Element parent, String ns, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && ns.equals(child.getNamespaceURI())
                    && localName.equals(child.getLocalName())) {
                result.add((Element) child);
            }
        }
        return result;
    }

    private static String collectRowText(Element row) {
        StringBuilder sb = new StringBuilder();
        NodeList tNodes = row.getElementsByTagNameNS(NS_W, "t");
        for (int i = 0; i < tNodes.getLength(); i++) {
            String text = tNodes.item(i).getTextContent();
            if (text != null) sb.append(text);
        }
        return sb.toString();
    }
}
