#!/bin/sh

#arm,mips,x86
PLAT=arm

if [ ${PLAT} == "arm" ]; then
  TARCC=arm-develop-linux-gnueabi-gcc
  TARARCH=arm-develop
elif [ ${PLAT} == "mips" ]; then
  TARCC=mips-en751221-linux-gnu-gcc
  TARARCH=mips-en751221
elif [ ${PLAT} == "x86" ]; then
  TARCC=gcc
  TARARCH=
fi

EXSH=`pwd`/$0
EXDIR=$(dirname ${EXSH})

PRETARGET=/tmp/transite-target

cfile=com_ymbl_smartgateway_extension_IpTables.c
hfile=com_ymbl_smartgateway_extension_IpTables.h

cd ${EXDIR}

echo "git iptables..."
[ ! -d "iptables" ] && \
    git clone -b v1.4.21 git://git.netfilter.org/iptables && \
    rm -rf iptables/.git && \
    patch -p0 < patches/iptables-jni-1.4.21.patch && \
    cp -v ${cfile} iptables/iptables/ && \
    cp -v ${hfile} iptables/iptables/ && \
    cp -v dlog.h iptables/iptables/

echo "git xl2tpd..."
[ ! -d "xl2tpd" ] && \
    git clone -b v1.3.6 https://github.com/xelerance/xl2tpd.git && \
    rm -rf xl2tpd/.git && \
    patch -p0 < patches/xl2tpd-jni-1.3.6.patch

echo "curl ppp..."
[ ! -d "ppp" ] && \
    curl -R -O https://download.samba.org/pub/ppp/ppp-2.4.7.tar.gz && \
    tar xf ppp-2.4.7.tar.gz && \
    rm -rf ppp-2.4.7.tar.gz && \
    mv ppp-2.4.7 ppp && \
    patch -p0 < patches/ppp-jni-2.4.7.patch

echo "curl lua..."
[ ! -d "lua" ] && \
    curl -R -O http://www.lua.org/ftp/lua-5.1.5.tar.gz && \
    tar xf lua-5.1.5.tar.gz && \
    rm -rf lua-5.1.5.tar.gz && \
    mv lua-5.1.5 lua && \
    patch -p0 < patches/lua-jni-5.1.5.patch

isnew=`find ${cfile} -newer iptables/iptables/${cfile}`
[ "$isnew" == "${cfile}" ] && \
    cp -v ${cfile} iptables/iptables/

isnew=`find ${hfile} -newer iptables/iptables/${hfile}`
[ "$isnew" == "${hfile}" ] && \
    cp -v ${hfile} iptables/iptables/

isnew=`find dlog.h -newer iptables/iptables/dlog.h`
[ "$isnew" == "dlog.h" ] && \
    cp -v dlog.h iptables/iptables/

echo "compiling..."
for i in ppp iptables RedirectToSock5Service;
do
  cd $i
  [ ! -x "configure" ] && ./autogen.sh
  isnew=`find ../compile.sh -newer configure`
  (([ "$isnew" == "../compile.sh" ] && make clean) || [ ! -f "Makefile" ]) && touch configure && ( \
    if [ ${PLAT} == "arm" ]; then ./configure --prefix=${PRETARGET} --host=arm-develop-linux-gnueabi CXXFLAGS=-I${EXDIR}/$i/extra/include LDFLAGS=-L${EXDIR}/$i/extra/lib/arm-develop; \
    elif [ ${PLAT} == "mips" ]; then ./configure --prefix=${PRETARGET} --host=mips-en751221-linux-gnu CXXFLAGS=-I${EXDIR}/$i/extra/include LDFLAGS=-L${EXDIR}/$i/extra/lib/mips-en751221; \
    elif [ ${PLAT} == "x86" ]; then ./configure --prefix=${PRETARGET}; fi)

  make CC=${TARCC} && make install
  rm -rf ${EXDIR}/$i/target
  mv ${PRETARGET} ${EXDIR}/$i/target
  cd ../
done


isnew=`find compile.sh -newer xl2tpd/Makefile`
[ "$isnew" == "compile.sh" ] && make -C xl2tpd clean && touch xl2tpd/Makefile
make -C xl2tpd CC=${TARCC} DESTDIR=${EXDIR}/xl2tpd/target CFLAGS="-DDEBUG_PPPD -DTRUST_PPPD_TO_DIE -DSANITY -DLINUX -DUSE_KERNEL -DIP_ALLOCATION -I${EXDIR}/libpcap/include" LDFLAGS=-L${EXDIR}/libpcap/lib/${TARARCH}

isnew=`find compile.sh -newer lua/src/Makefile`
[ "$isnew" == "compile.sh" ] && make -C lua/src clean && touch lua/src/Makefile
make -C lua/src linux CC=${TARCC} CFLAGS="-DLUA_USE_LINUX -DNO_GETLOGIN -fPIC -std=gnu99 -I${EXDIR}/readline/include" LDFLAGS="-L${EXDIR}/readline/lib/${TARARCH} -L${EXDIR}/ncurses/lib/${TARARCH}"

echo "copying..."
cp -v ${EXDIR}/iptables/target/lib/libtransite.so.0.1.0 ../src/main/java/IpTables.so
cp -v ${EXDIR}/iptables/target/lib/libip4tc.so.0 ../src/main/java/
cp -v ${EXDIR}/iptables/target/lib/libip6tc.so.0 ../src/main/java/
cp -v ${EXDIR}/iptables/target/lib/libxtables.so.10 ../src/main/java/
cp -av ${EXDIR}/iptables/target/lib/xtables ../src/main/java/
cp -v ${EXDIR}/RedirectToSock5Service/target/lib/librectsocks5.so.0.1.0 ../src/main/java/RedirectToSocks5Service.so
cp -v ${EXDIR}/xl2tpd/xl2tpd ../src/main/java/
cp -v ${EXDIR}/ppp/target/sbin/pppd ../src/main/java/
cp -v ${EXDIR}/ppp/target/lib/pppd/2.4.7/pppol2tp.so ../src/main/java/
cp -v ${EXDIR}/ppp/target/lib/pppd/2.4.7/openl2tp.so ../src/main/java/
cp -v ${EXDIR}/lua/src/lua ../src/main/java/
