
create table categorias (
    id                 bigint auto_increment,
    nombre             varchar(50)  not null,
    descripcion        varchar(200),
    estado             bit(1)            not null,
    fecha_creacion     datetime(6)       not null,
    fecha_modificacion datetime(6),
    constraint pk_categorias primary key (id)
);

create unique index ux_categorias_nombre on categorias ((upper(nombre)));

create table clientes (
    id                 bigint auto_increment,
    dni                varchar(8)   not null,
    nombres            varchar(100) not null,
    apellidos          varchar(100) not null,
    email              varchar(150) not null,
    telefono           varchar(15),
    direccion          varchar(250),
    estado             bit(1)            not null,
    fecha_creacion     datetime(6)       not null,
    fecha_modificacion datetime(6),
    constraint pk_clientes primary key (id),
    constraint ux_clientes_dni unique (dni)
);

create unique index ux_clientes_email on clientes ((upper(email)));

create table productos (
    id                 bigint auto_increment,
    nombre             varchar(150) not null,
    precio             decimal(10,2)       not null,
    stock              int       not null,
    estado             bit(1)            not null,
    categoria_id       bigint       not null,
    fecha_creacion     datetime(6)       not null,
    fecha_modificacion datetime(6),
    constraint pk_productos primary key (id),
    constraint fk_productos_categoria
        foreign key (categoria_id) references categorias (id),
    constraint ck_productos_stock check (stock >= 0)
);

create unique index ux_productos_nombre on productos ((upper(nombre)));
create index ix_productos_categoria on productos (categoria_id);

create table ventas (
    id         bigint auto_increment,
    fecha      datetime(6)      not null,
    total      decimal(12,2)      not null,
    estado     varchar(20) not null,
    cliente_id bigint      not null,
    constraint pk_ventas primary key (id),
    constraint fk_ventas_cliente
        foreign key (cliente_id) references clientes (id),
    constraint ck_ventas_estado
        check (estado in ('REGISTRADA', 'ANULADA'))
);

create index ix_ventas_cliente on ventas (cliente_id);
create index ix_ventas_fecha   on ventas (fecha);

create table detalle_ventas (
    id          bigint auto_increment,
    venta_id    bigint not null,
    producto_id bigint not null,
    cantidad    int not null,
    precio      decimal(10,2) not null,
    subtotal    decimal(12,2) not null,
    constraint pk_detalle_ventas primary key (id),
    constraint fk_detalle_ventas_venta
        foreign key (venta_id) references ventas (id),
    constraint fk_detalle_ventas_producto
        foreign key (producto_id) references productos (id),
    constraint ck_detalle_ventas_cantidad check (cantidad > 0)
);

create index ix_detalle_ventas_venta    on detalle_ventas (venta_id);
create index ix_detalle_ventas_producto on detalle_ventas (producto_id);