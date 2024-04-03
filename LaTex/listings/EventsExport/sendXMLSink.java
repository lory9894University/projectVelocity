public class sendXMLSink implements SinkFunction<ExportEventToSend> {
    private MailboxService mailboxService;

    void SendXMLSink(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }
@Override
    public void invoke(ExportEventToSend value, Context context) throws Exception {
        String documentID = mailboxService.createDocument(value.getXmlMessage());
        mailboxService.addMessageToMailBox(value.getFileName(), documentID);
    }
}