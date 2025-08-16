package de.fuh.kn.webapp.common.mapper;

/**
 * Basis-Interface für Entity-DTO-Mapper.
 * Definiert grundlegende Mapping-Methoden, die in allen spezifischen Mappern verwendet werden können.
 *
 * @param <E> Entitätstyp
 * @param <D> DTO-Typ
 */
public interface EntityDtoMapper<E, D> {

    /**
     * Konvertiert eine Entität in ein DTO.
     *
     * @param entity Die Entität, die konvertiert werden soll
     * @return Das resultierende DTO
     */
    D toDto(E entity);

    /**
     * Konvertiert ein DTO in eine Entität.
     *
     * @param dto Das DTO, das konvertiert werden soll
     * @return Die resultierende Entität
     */
    E toEntity(D dto);
}
