/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.utils;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author /
 */
@Component
@SuppressWarnings({"unchecked","all"})
public class RedisUtils {
    private static final Logger log = LoggerFactory.getLogger(RedisUtils.class);

    private RedisTemplate<Object, Object> redisTemplate;

    public RedisUtils(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        this.redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    }

    /**
     * Specify cache expiration time
     *
     * @param key  Key
     * @param time Time in seconds. Note: this will override the existing expiration time
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Specify cache expiration time
     *
     * @param key      Key
     * @param time     Time in seconds. Note: this will override the existing expiration time
     * @param timeUnit Time unit
     */
    public boolean expire(String key, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, timeUnit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Get expiration time by key
     *
     * @param key Key, cannot be null
     * @return Expiration time in seconds; 0 indicates permanent validity
     */
    public long getExpire(Object key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * Find matching keys
     *
     * @param pattern key
     * @return /
     */
    public List<String> scan(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection rc = Objects.requireNonNull(factory).getConnection();
        Cursor<byte[]> cursor = rc.scan(options);
        List<String> result = new ArrayList<>();
        while (cursor.hasNext()) {
            result.add(new String(cursor.next()));
        }
        try {
            RedisConnectionUtils.releaseConnection(rc, factory);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Paginated query for keys
     *
     * @param patternKey key
     * @param page       Page index
     * @param size       Page size
     * @return /
     */
    public List<String> findKeysForPage(String patternKey, int page, int size) {
        ScanOptions options = ScanOptions.scanOptions().match(patternKey).build();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection rc = Objects.requireNonNull(factory).getConnection();
        Cursor<byte[]> cursor = rc.scan(options);
        List<String> result = new ArrayList<>(size);
        int tmpIndex = 0;
        int fromIndex = page * size;
        int toIndex = page * size + size;
        while (cursor.hasNext()) {
            if (tmpIndex >= fromIndex && tmpIndex < toIndex) {
                result.add(new String(cursor.next()));
                tmpIndex++;
                continue;
            }
            // Once matching data is retrieved, exit the loop
            if (tmpIndex >= toIndex) {
                break;
            }
            tmpIndex++;
            cursor.next();
        }
        try {
            RedisConnectionUtils.releaseConnection(rc, factory);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Determine whether key exists
     *
     * @param key Key
     * @return true if exists, false otherwise
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete cache
     *
     * @param key One or more keys
     */
    public void del(String... keys) {
        if (keys != null && keys.length > 0) {
            if (keys.length == 1) {
                boolean result = redisTemplate.delete(keys[0]);
                log.debug("--------------------------------------------");
                log.debug(new StringBuilder("Delete cache: ").append(keys[0]).append(", result: ").append(result).toString());
                log.debug("--------------------------------------------");
            } else {
                Set<Object> keySet = new HashSet<>();
                for (String key : keys) {
                    if (redisTemplate.hasKey(key))
                        keySet.add(key);
                }
                long count = redisTemplate.delete(keySet);
                log.debug("--------------------------------------------");
                log.debug("Successfully deleted cache: " + keySet.toString());
                log.debug("Deleted cache count: " + count);
                log.debug("--------------------------------------------");
            }
        }
    }

    /**
     * Batch fuzzy delete keys
     * @param pattern
     */
    public void scanDel(String pattern){
        ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
        try (Cursor<byte[]> cursor = redisTemplate.executeWithStickyConnection(
                (RedisCallback<Cursor<byte[]>>) connection -> (Cursor<byte[]>) new ConvertingCursor<>(
                        connection.scan(options), redisTemplate.getKeySerializer()::deserialize))) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }
    }

    // ============================String=============================

    /**
     * Get normal cache
     *
     * @param key Key
     * @return Value
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * Get normal cache
     *
     * @param key Key
     * @return Value
     */
    public <T> T get(String key, Class<T> clazz) {
        Object value = key == null ? null : redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        // If value is not the target type, attempt to deserialize it to clazz type
        if (!clazz.isInstance(value)) {
            return JSON.parseObject(value.toString(), clazz);
        } else if (clazz.isInstance(value)) {
            return clazz.cast(value);
        } else {
            return null;
        }
    }

    /**
     * Get normal cache
     *
     * @param key Key
     * @param clazz Element type in the list
     * @return Value
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        Object value = key == null ? null : redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            // Check if each element is of the specified type
            if (list.stream().allMatch(clazz::isInstance)) {
                return list.stream().map(clazz::cast).collect(Collectors.toList());
            }
        }
        return null;
    }


    /**
     * Get normal cache
     *
     * @param key Key
     * @return Value
     */
    public String getStr(String key) {
        if(StrUtil.isBlank(key)){
            return null;
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Batch get
     *
     * @param keys
     * @return
     */
    public List<Object> multiGet(List<String> keys) {
        List list = redisTemplate.opsForValue().multiGet(Sets.newHashSet(keys));
        List resultList = Lists.newArrayList();
        Optional.ofNullable(list).ifPresent(e-> list.forEach(ele-> Optional.ofNullable(ele).ifPresent(resultList::add)));
        return resultList;
    }

    /**
     * Put normal cache
     *
     * @param key   Key
     * @param value Value
     * @return true if successful, false otherwise
     */
    public boolean set(String key, Object value) {
        int attempt = 0;
        while (attempt < 3) {
            try {
                redisTemplate.opsForValue().set(key, value);
                return true;
            } catch (Exception e) {
                attempt++;
                log.error("Attempt {} failed: {}", attempt, e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Put normal cache with expiration time
     *
     * @param key   Key
     * @param value Value
     * @param time  Time in seconds; must be > 0. If <= 0, infinite duration is set. Note: overrides existing expiration
     * @return true if successful, false otherwise
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Put normal cache with expiration time
     *
     * @param key      Key
     * @param value    Value
     * @param time     Time. Note: overrides existing expiration
     * @param timeUnit Time unit type
     * @return true if successful, false otherwise
     */
    public boolean set(String key, Object value, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    // ================================Map=================================

    /**
     * HashGet
     *
     * @param key  Key, cannot be null
     * @param item Item, cannot be null
     * @return Value
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * Get all key-value pairs for the specified hash key
     *
     * @param key Key
     * @return Corresponding key-value pairs
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);

    }

    /**
     * HashSet
     *
     * @param key Key
     * @param map Corresponding key-value pairs
     * @return true if successful, false otherwise
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * HashSet
     *
     * @param key  Key
     * @param map  Corresponding key-value pairs
     * @param time Time in seconds. Note: overrides existing expiration if hash table exists
     * @return true if successful, false otherwise
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Put data into a hash table; create if not exists
     *
     * @param key   Key
     * @param item  Item
     * @param value Value
     * @return true if successful, false otherwise
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Put data into a hash table; create if not exists
     *
     * @param key   Key
     * @param item  Item
     * @param value Value
     * @param time  Time in seconds. Note: overrides existing expiration if hash table exists
     * @return true if successful, false otherwise
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete values from a hash table
     *
     * @param key  Key, cannot be null
     * @param item One or more items, cannot be null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * Check whether the item exists in the hash table
     *
     * @param key  Key, cannot be null
     * @param item Item, cannot be null
     * @return true if exists, false otherwise
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * Increment hash item; create if not exists and return incremented value
     *
     * @param key  Key
     * @param item Item
     * @param by   Amount to increment (greater than 0)
     * @return
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * Decrement hash item
     *
     * @param key  Key
     * @param item Item
     * @param by   Amount to decrement (less than 0)
     * @return
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ============================set=============================

    /**
     * Get all values from Set by key
     *
     * @param key Key
     * @return
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check whether value exists in Set
     *
     * @param key   Key
     * @param value Value
     * @return true if exists, false otherwise
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Put data into Set cache
     *
     * @param key    Key
     * @param values One or more values
     * @return Number of successfully added items
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Put Set data into cache
     *
     * @param key    Key
     * @param time   Time in seconds. Note: overrides existing expiration time
     * @param values One or more values
     * @return Number of successfully added items
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get size of Set cache
     *
     * @param key Key
     * @return
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Remove elements with value
     *
     * @param key    Key
     * @param values One or more values
     * @return Number of removed items
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    // ===============================list=================================

    /**
     * Get contents of List cache
     *
     * @param key   Key
     * @param start Start index
     * @param end   End index; 0 to -1 represents all values
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get size of List cache
     *
     * @param key Key
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get value from List by index
     *
     * @param key   Key
     * @param index Index. When index >= 0: 0 is head, 1 is second element, etc.; when index < 0: -1 is tail, -2 is second to last element, etc.
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Put list into cache
     *
     * @param key   Key
     * @param value Value
     * @return
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Put list into cache
     *
     * @param key   Key
     * @param value Value
     * @param time  Time in seconds. Note: overrides existing expiration time
     * @return
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Put list into cache
     *
     * @param key   Key
     * @param value Value
     * @return
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Put list into cache
     *
     * @param key   Key
     * @param value Value
     * @param time  Time in seconds. Note: overrides existing expiration time
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update an item in List cache by index
     *
     * @param key   Key
     * @param index Index
     * @param value Value
     * @return /
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Remove N items with value
     *
     * @param key   Key
     * @param count Number of items to remove
     * @param value Value
     * @return Number of removed items
     */
    public long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * @param prefix Prefix
     * @param ids    id
     */
    public void delByKeys(String prefix, Set<Long> ids) {
        Set<Object> keys = new HashSet<>();
        for (Long id : ids) {
            keys.addAll(redisTemplate.keys(new StringBuffer(prefix).append(id).toString()));
        }
        long count = redisTemplate.delete(keys);
    }

    // ============================incr=============================

    /**
     * Increment
     * @param key
     * @return
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * Decrement
     * @param key
     * @return
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }
}
