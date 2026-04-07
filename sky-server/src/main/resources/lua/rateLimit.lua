-- KEYS[1] 限流key
-- ARGV[1] 最大次数
-- ARGV[2] 过期秒数

local count = redis.call("get", KEYS[1])

if count and tonumber(count) >= tonumber(ARGV[1]) then
    return 0 -- 超过限制，限流
end

-- 计数器+1
local after = redis.call("incr", KEYS[1])

-- 第一次设置过期时间
if after == 1 then
    redis.call("expire", KEYS[1], ARGV[2])
end

return 1 -- 放行