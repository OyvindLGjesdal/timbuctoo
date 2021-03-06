package nl.knaw.huygens.security.client;

/*
 * #%L
 * Security Client
 * =======
 * Copyright (C) 2013 - 2014 Huygens ING
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

import nl.knaw.huygens.security.client.model.SecurityInformation;

import java.io.IOException;

/**
 * Get the data from a ContainerRequest, that is required for the
 * SecurityInformation. This interface should be used to communicate with 3rd
 * party security services.
 */
public interface AuthenticationHandler {

  /**
   * Extracts the information for needed for creating a SecurityContext from a
   * ContainerRequest. The implementation of this interface will be dependent
   * on the 3rd party security implementation. 
   * @param sessionId the id of the user session.
   * 
   * @return the information needed to create a SecurityContext.
   * @throws UnauthorizedException will be thrown when the @{code sessionId} is null or invalid.
   */
  SecurityInformation getSecurityInformation(String sessionId) throws UnauthorizedException, IOException;
}
