package com.example.openfirechat;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private ArrayList<MessagesData> mMessagesData = new ArrayList<>();
    private AbstractXMPPConnection mConnection;
    public static final String TAG = MainActivity.class.getSimpleName();
    private EditText sendMessageEt;
    private Button sendBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.rv);
        mAdapter = new Adapter(mMessagesData);
        sendMessageEt = findViewById(R.id.sendMessageEt);
        sendBtn = findViewById(R.id.send);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        DividerItemDecoration decoration = new DividerItemDecoration(this, manager.getOrientation());

        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(mAdapter);

        setConnection();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageSend = sendMessageEt.getText().toString();
                if (messageSend.length() > 0){

                    sendMessage(messageSend, "user_test1@192.168.1.103");
                }
            }
        });
    }

    private void sendMessage(String messageSend, String entityBareId) {
        EntityBareJid jid = null;
        try {
            jid = JidCreate.entityBareFrom(entityBareId);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        if (mConnection != null){
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            Chat chat = chatManager.chatWith(jid);
            Message newMessage = new Message();
            newMessage.setBody(messageSend);

            try {
                chat.send(newMessage);

                MessagesData data = new MessagesData("send",messageSend);
                mMessagesData.add(data);
                mAdapter = new Adapter(mMessagesData);
                mRecyclerView.setAdapter(mAdapter);

            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void setConnection(){

        new Thread(){
            @Override
            public void run() {

        InetAddress addr = null;
        try {
            addr = InetAddress.getByName("192.168.1.103");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return false;
            }
        };
        DomainBareJid serviceName = null;
        try {
            serviceName = JidCreate.domainBareFrom("192.168.1.103");
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()

              .setUsernameAndPassword("user_test2","Adriana1988")
              .setPort(5222)
              .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
              .setXmppDomain(serviceName)
              .setHostnameVerifier(verifier)
              .setHostAddress(addr)
              .setDebuggerEnabled(true)
              .build();
        mConnection = new XMPPTCPConnection(config);

        // Create the configuration for this new connection
        try {
            mConnection.connect();
            mConnection.login();
            if(mConnection.isAuthenticated() && mConnection.isConnected()){

                Log.e(TAG,"run:auth donde and connected succesfully");
                ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
                chatManager.addIncomingListener(new IncomingChatMessageListener() {
                    @Override
                    public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                        Log.e(TAG,"New message from " + from + ": " + message.getBody());

                        MessagesData data = new MessagesData("received",message.getBody().toString());
                        mMessagesData.add(data);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter = new Adapter(mMessagesData);
                                mRecyclerView.setAdapter(mAdapter);
                            }
                        });
                    }
                });
            }


        } catch (SmackException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMPPException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
                super.run();
            }
        }.start();

    }
}
