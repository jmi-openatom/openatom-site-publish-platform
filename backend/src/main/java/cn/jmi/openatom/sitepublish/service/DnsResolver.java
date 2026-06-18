package cn.jmi.openatom.sitepublish.service;

import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Component
public class DnsResolver {

    public List<String> lookupCname(String domain) {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        environment.put("com.sun.jndi.dns.timeout.initial", "2000");
        environment.put("com.sun.jndi.dns.timeout.retries", "1");

        List<String> records = new ArrayList<>();
        try {
            DirContext context = new InitialDirContext(environment);
            try {
                Attributes attributes = context.getAttributes(domain, new String[]{"CNAME"});
                Attribute cname = attributes.get("CNAME");
                if (cname == null) {
                    return records;
                }
                NamingEnumeration<?> values = cname.getAll();
                while (values.hasMore()) {
                    records.add(String.valueOf(values.next()));
                }
            } finally {
                context.close();
            }
            return records;
        } catch (Exception exception) {
            return records;
        }
    }
}
