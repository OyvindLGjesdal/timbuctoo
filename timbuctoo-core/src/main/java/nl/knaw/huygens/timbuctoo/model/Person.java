package nl.knaw.huygens.timbuctoo.model;

/*
 * #%L
 * Timbuctoo core
 * =======
 * Copyright (C) 2012 - 2014 Huygens ING
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.List;

import nl.knaw.huygens.timbuctoo.annotations.IDPrefix;
import nl.knaw.huygens.timbuctoo.facet.IndexAnnotation;
import nl.knaw.huygens.timbuctoo.model.util.Datable;
import nl.knaw.huygens.timbuctoo.model.util.Link;
import nl.knaw.huygens.timbuctoo.model.util.PersonName;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

@IDPrefix("PERS")
public class Person extends DomainEntity {

  @JsonFormat(shape = JsonFormat.Shape.OBJECT)
  public static enum Gender {
    UNKNOWN, MALE, FEMALE, NOT_APPLICABLE;
  }

  // start for modeling of roles
  @JsonFormat(shape = JsonFormat.Shape.OBJECT)
  public static enum Type {
    UNKNOWN, ARCHETYPE, AUTHOR, PSEUDONYM
  }

  private List<Type> types;
  private PersonName name;
  /** Gender at birth. */
  private Gender gender;
  private Datable birthDate;
  private Datable deathDate;
  private List<Link> links;

  public Person() {
    name = new PersonName();
    links = Lists.newArrayList();
    types = Lists.newArrayList();
  }

  @Override
  public String getDisplayName() {
    return name.getShortName();
  }

  @JsonIgnore
  @IndexAnnotation(fieldName = "dynamic_t_name", isFaceted = false)
  public String getIndexedName() {
    return name.getFullName();
  }

  public List<Type> getTypes() {
    return types;
  }

  public void setTypes(List<Type> types) {
    this.types = types;
  }

  public void addType(Type type) {
    if (type != null && !types.contains(type)) {
      types.add(type);
    }
  }

  public PersonName getName() {
    return name;
  }

  public void setName(PersonName name) {
    this.name = name;
  }

  @IndexAnnotation(fieldName = "dynamic_s_gender", isFaceted = true, canBeEmpty = true)
  public Gender getGender() {
    return gender;
  }

  public void setGender(Gender gender) {
    this.gender = gender;
  }

  @IndexAnnotation(fieldName = "dynamic_s_birthDate", isFaceted = true, canBeEmpty = true)
  public Datable getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(Datable birthDate) {
    this.birthDate = birthDate;
  }

  @IndexAnnotation(fieldName = "dynamic_s_deathDate", isFaceted = true, canBeEmpty = true)
  public Datable getDeathDate() {
    return deathDate;
  }

  public void setDeathDate(Datable deathDate) {
    this.deathDate = deathDate;
  }

  public List<Link> getLinks() {
    return links;
  }

  public void setLinks(List<Link> links) {
    this.links = links;
  }

  public void addLink(Link link) {
    if (link != null) {
      links.add(link);
    }
  }

}
