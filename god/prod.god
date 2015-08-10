inner_ip = "10.100.10.45"
outer_ip = "123.103.10.130"

(0..9).each do |i|
    God.watch do |w|
        w.group = "comet"
        w.name = "comet-#{i}"
        w.start = %Q(mvn exec:java -Pcomet -Dexec.args="--runmode prod --maxconn 100000 --seq #{i} --cometip #{outer_ip} --rpcip #{inner_ip}")
        w.log = "/data/logs/hipush/comet-#{i}.log"
        w.env = { 'MALLOC_ARENA_MAX' => '4',
                  'MAVEN_OPTS' => '-Xms512m -Xmx1024m',
                  'serverIdStart' => '0',
                  'portStart' => '20024'}
        w.dir = "."
        w.behavior(:clean_pid_file)
    end
end


(0..4).each do |i|
    God.watch do |w|
        w.group = "admin"
        w.name = "admin-#{i}"
        w.start = %Q(mvn exec:java -Padmin -Dexec.args="--runmode prod --seq #{i} --ip #{inner_ip}")
        w.log = "/data/logs/hipush/admin-#{i}.log"
        w.env = { 'MALLOC_ARENA_MAX' => '4',
                  'MAVEN_OPTS' => '-Xms256m -Xmx512m',
                  'serverIdStart' => '0',
                  'portStart' => '22024'}
        w.dir = "."
        w.behavior(:clean_pid_file)
    end
end

(0..4).each do |i|
    God.watch do |w|
        w.group = "web"
        w.name = "web-#{i}"
        w.start = %Q(mvn exec:java -Pweb -Dexec.args="--runmode prod --seq #{i} --ip #{inner_ip}")
        w.log = "/data/logs/hipush/web-#{i}.log"
        w.env = { 'MALLOC_ARENA_MAX' => '4',
                  'MAVEN_OPTS' => '-Xms256m -Xmx512m',
                  'serverIdStart' => '0',
                  'portStart' => '21024'}
        w.dir = "."
        w.behavior(:clean_pid_file)
    end
end

God.watch do |w|
    w.name = "dog"
    w.start = %Q(mvn exec:java -Pdog -Dexec.args="--runmode prod --upstream /etc/nginx/upstream.conf")
    w.log = "/data/logs/hipush/dog.log"
    w.env = { 'MALLOC_ARENA_MAX' => '4',
              'MAVEN_OPTS' => '-Xms64m -Xmx128m' }
    w.dir = "."
    w.behavior(:clean_pid_file)
end