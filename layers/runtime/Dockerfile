FROM amazonlinux:2

WORKDIR /build/
RUN yum install -y yum-utils && \
    yum install -y gzip tar.x86_64 && \
    yum -y update && \
    yum -y group install "development tools" && \
    yum install -y clang llvm llvm-devel && \
    yum install -y zip which libunwind libunwind-devel python-pip jq libcurl-devel && \
    pip install awscli && \
    yum install -y libidn2 libidn2-devel openssl11-libs.x86_64

WORKDIR /build/openssl
RUN git clone https://github.com/openssl/openssl.git -b OpenSSL_1_1_1t --depth=1 && \
    cd openssl && \
    ./config && make && make install && \
    rm -rf /build/openssl

WORKDIR /build/s2n
RUN yum install -y gcc-c++ wget && \
    wget https://cmake.org/files/v3.22/cmake-3.22.1.tar.gz && \
    tar -xvzf cmake-3.22.1.tar.gz && \
    cd cmake-3.22.1 && \
    ./bootstrap && make && make install

RUN git clone https://github.com/aws/s2n-tls.git && \
    cd s2n-tls && \
    cmake . -Bbuild -DCMAKE_EXE_LINKER_FLAGS="-lcrypto -lz" -DCMAKE_EXPORT_COMPILE_COMMANDS=ON && \
    cmake --build ./build -j $(nproc) && \
    cmake --install ./build
RUN ldconfig
RUN rm -rf /build/s2n/

WORKDIR /build/runtime/
RUN mkdir -p /build/runtime/lib/ && \
    ls /usr/lib64 | grep -E "lib(crypto|curl|unwind|unwind-x86_64|idn|ssl).so" | xargs -I{} cp "/usr/lib64/{}" /build/runtime/lib/

RUN curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh
ENV PATH=$PATH:/root/.cache/scalacli/local-repo/bin/scala-cli
WORKDIR /build/warm/
RUN echo 'println("Hello world")' | scala-cli run - --native --native-version=0.4.12 -S 3.3.0-RC5 
RUN rm -rf /build/warm/

ADD bootstrap /build/runtime/

ENV LD_LIBRARY_PATH=/opt/lib/ssl/lib:/usr/local/lib:$LD_LIBRARY_PATH 
WORKDIR /build/main/
