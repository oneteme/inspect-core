package org.usf.inspect.redis;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class RespParser {

    private static final byte ARRAY = '*';
    private static final byte BULK_STRING = '$';
    private static final byte CR = '\r';

    private static final byte SIMPLE_STRING = '+';
    private static final byte ERROR = '-';
    private static final byte INTEGER = ':';

    private RespParser() {
    }

    /**
     * Parse une trame RESP (requête Redis) sans consommer le ByteBuf.
     *
     * @param buf buffer contenant la trame RESP
     * @return la commande et ses arguments, ou null si la trame est illisible/incomplète
     */
    public static RespCommand parse(ByteBuf buf) {
        if (buf == null || !buf.isReadable()) {
            return null;
        }
        // duplicate() : index de lecture indépendants -> le buffer original n'est pas consommé
        var dup = buf.duplicate();
        try {
            if (dup.readByte() != ARRAY) {
                return null; // on ne gère que les tableaux RESP (requêtes Redisson)
            }
            int count = readInteger(dup);
            if (count <= 0) {
                return null;
            }
            var tokens = new ArrayList<String>(count);
            for (int i = 0; i < count; i++) {
                tokens.add(readBulkString(dup));
            }
            var command = tokens.get(0) == null ? null : tokens.get(0).toUpperCase();
            var args = tokens.subList(1, tokens.size()).toArray(String[]::new);
            return new RespCommand(RedisCommand.parse(command), args);
        }
        catch (IndexOutOfBoundsException e) {
            return null; // trame incomplète ou malformée
        }
    }

    /**
     * Parse une réponse RESP (Redis -> Redisson) sans consommer le ByteBuf.
     *
     * @return la réponse typée, ou null si la trame est illisible/fragmentée
     */
    public static RespReply parseResponse(ByteBuf buf) {
        if (buf == null || !buf.isReadable()) {
            return null;
        }
        var dup = buf.duplicate();
        try {
            return readReply(dup);
        }
        catch (IndexOutOfBoundsException e) {
            return null; // trame incomplète ou fragmentée (TCP)
        }
    }

    private static RespReply readReply(ByteBuf buf) {
        byte type = buf.readByte();
        return switch (type) {
            case SIMPLE_STRING -> new RespReply(RespReply.RespType.SIMPLE_STRING, readLine(buf));
            case ERROR -> new RespReply(RespReply.RespType.ERROR, readLine(buf));
            case INTEGER -> new RespReply(RespReply.RespType.INTEGER, readLong(buf));
            case BULK_STRING -> readBulk(buf);
            case ARRAY -> readArray(buf);
            default -> null; // type RESP non géré
        };
    }

    private static RespReply readBulk(ByteBuf buf) {
        int length = readInteger(buf);
        if (length < 0) {
            return new RespReply(RespReply.RespType.NULL, null); // $-1
        }
        var bytes = new byte[length];
        buf.readBytes(bytes);
        buf.skipBytes(2); // CRLF final
        return new RespReply(RespReply.RespType.BULK_STRING, new String(bytes, StandardCharsets.UTF_8));
    }

    private static RespReply readArray(ByteBuf buf) {
        int count = readInteger(buf);
        if (count < 0) {
            return new RespReply(RespReply.RespType.NULL, null); // *-1
        }
        var items = new ArrayList<RespReply>(count);
        for (int i = 0; i < count; i++) {
            items.add(readReply(buf)); // parsing récursif (arrays imbriqués)
        }
        return new RespReply(RespReply.RespType.ARRAY, items);
    }

    private static String readLine(ByteBuf buf) {
        var sb = new StringBuilder();
        byte b = buf.readByte();
        while (b != CR) {
            sb.append((char) b);
            b = buf.readByte();
        }
        buf.readByte(); // saute le LF
        return sb.toString();
    }

    private static long readLong(ByteBuf buf) {
        long value = 0;
        boolean negative = false;
        byte b = buf.readByte();
        if (b == '-') {
            negative = true;
            b = buf.readByte();
        }
        while (b != CR) {
            value = value * 10 + (b - '0');
            b = buf.readByte();
        }
        buf.readByte(); // saute le LF
        return negative ? -value : value;
    }

    private static String readBulkString(ByteBuf buf) {
        if (buf.readByte() != BULK_STRING) {
            throw new IndexOutOfBoundsException("Bulk string attendu");
        }
        int length = readInteger(buf);
        if (length < 0) {
            return null; // null bulk string ($-1)
        }
        var bytes = new byte[length];
        buf.readBytes(bytes);
        buf.skipBytes(2); // saute le CRLF final
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int readInteger(ByteBuf buf) {
        int value = 0;
        boolean negative = false;
        byte b = buf.readByte();
        if (b == '-') {
            negative = true;
            b = buf.readByte();
        }
        while (b != CR) {
            value = value * 10 + (b - '0');
            b = buf.readByte();
        }
        buf.readByte(); // saute le LF
        return negative ? -value : value;
    }

}
