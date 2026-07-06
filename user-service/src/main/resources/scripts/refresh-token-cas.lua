local current = redis.call('GET', KEYS[1])

if current == false then
    return 0
end

if current ~= ARGV[1] then
    return -1
end

redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
return 1