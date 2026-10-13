
create table empleados (
    id                 bigint auto_increment,
    dni                varchar(8)   not null,
    nombres            varchar(100) not null,
    apellidos          varchar(100) not null,
    cargo              varchar(60)  not null,
    email              varchar(150) not null,
    telefono           varchar(15),
    estado             bit(1)            not null,
    fecha_creacion     datetime(6)       not null,
    fecha_modificacion datetime(6),
    constraint pk_empleados primary key (id),
    constraint ux_empleados_dni unique (dni)
);

create unique index ux_empleados_email on empleados ((upper(email)));

create table roles (
    id          bigint auto_increment,
    nombre      varchar(30)  not null,
    descripcion varchar(150),
    constraint pk_roles primary key (id),
    constraint ux_roles_nombre unique (nombre)
);

create table usuarios (
    id                 bigint auto_increment,
    username           varchar(50)  not null,
    password           varchar(100) not null,
    estado             bit(1)            not null,
    empleado_id        bigint       not null,
    fecha_creacion     datetime(6)       not null,
    fecha_modificacion datetime(6),
    constraint pk_usuarios primary key (id),
    constraint ux_usuarios_empleado unique (empleado_id),
    constraint fk_usuarios_empleado foreign key (empleado_id) references empleados (id)
);

create unique index ux_usuarios_username on usuarios ((upper(username)));

create table usuario_roles (
    usuario_id bigint not null,
    rol_id     bigint not null,
    constraint pk_usuario_roles primary key (usuario_id, rol_id),
    constraint fk_usuario_roles_usuario foreign key (usuario_id) references usuarios (id),
    constraint fk_usuario_roles_rol foreign key (rol_id) references roles (id)
);

insert into roles (nombre, descripcion) values ('ADMIN', 'Administrador con todos los permisos');
insert into roles (nombre, descripcion) values ('VENDEDOR', 'Registra ventas y atiende clientes');
insert into roles (nombre, descripcion) values ('ALMACENERO', 'Gestiona productos y stock');