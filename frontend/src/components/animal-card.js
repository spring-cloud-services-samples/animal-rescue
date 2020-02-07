import React from 'react'
import {Button, Card, Icon, Image} from 'semantic-ui-react'

const AnimalCard = (props) => (
    <Card>
        <Card.Content>
            <Image
                floated='right'
                size='medium'
                src={props.animal.avatar}
            />
            <Card.Header>{props.animal.name}</Card.Header>
            <Card.Meta>
                <span className='date'>Rescued on {props.animal.rescueDate}</span>
            </Card.Meta>
            <Card.Description>
                {props.animal.description}
            </Card.Description>
        </Card.Content>
        <Card.Content extra>
            <div>
                <Icon name='user' />
                {props.animal.numPendingAdoptors} Pending Adoptors
            </div>
            <div className='ui buttons'>
                <Button basic color='green'>
                    Adopt
                </Button>
            </div>
        </Card.Content>
    </Card>
);

export default AnimalCard
