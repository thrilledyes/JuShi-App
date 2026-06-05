package model;

public class RawMessage {

    public long id;                // primary key for SQL
    public String sessionId;       // QQ_Group_1234,WX_Group_xxxx,ChaoXing_Course_259
    public SourceType source;      // QQ,WX,WEB...
    public String sender;          // Sender Name(QQ/WX),Website Name(WEB)
    public String title;           // Null(QQ/WX/Android), "xxx_homework/assignment"(WEB)
    public String content;         // Major Content
    public String rawPayload;      // Original Data
    public long timestamp;         // timestamp
}
