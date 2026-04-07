
-- 1. 获取参数
local dishId = KEYS[1]
local userId = KEYS[2]
local stockKey = "dish:stock:" .. dishId
local orderKey = "order:user:" .. userId .. ":" .. dishId

-- 2. 判断用户是否已经下单（一人一单）
if redis.call("exists", orderKey) == 1 then
    return 1  -- 已存在未支付订单，禁止重复下单
end

-- 3. 判断库存是否充足
local stock = tonumber(redis.call("get", stockKey))
if stock <= 0 then
    return 2  -- 库存不足
end

-- 4. 原子扣减库存
redis.call("decr", stockKey)
-- 5. 标记用户已下单（10分钟超时未支付自动释放）
redis.call("setex", orderKey, 600, "1")

return 0  -- 成功