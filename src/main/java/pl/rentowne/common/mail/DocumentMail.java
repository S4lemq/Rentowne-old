package pl.rentowne.common.mail;

import java.util.List;

public record DocumentMail(String to, List<String> cc, String subject, String text, String filename, byte[] pdf) {}
